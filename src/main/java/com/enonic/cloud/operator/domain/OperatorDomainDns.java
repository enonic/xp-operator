package com.enonic.cloud.operator.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.cloudflare.DnsRecordServiceWrapper;
import com.enonic.cloud.apis.cloudflare.service.model.DnsRecord;
import com.enonic.cloud.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.cloud.apis.doh.DohServiceWrapper;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatusFields;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

// TODO: REFACTOR THIS CLASS

/**
 * This operator class creates/updates/deletes DNS records in cloudflare based on Domains
 */
@Singleton
public class OperatorDomainDns
    extends InformerEventHandler<Domain>
{
    private final Logger log = LoggerFactory.getLogger( OperatorDomainDns.class );

    @Inject
    Clients clients;

    @Inject
    DohServiceWrapper doh;

    @Inject
    DnsRecordServiceWrapper dnsRecordService;

    @Inject
    RunnableListExecutor runnableListExecutor;

    @Inject
    @Named("clusterId")
    String clusterId;

    @Inject
    LbServiceIpProducer lbServiceIpProducer;

    @ConfigProperty(name = "dns.allowedDomains.keys")
    List<String> allowedDomainKeys;

    List<DomainConfig> allowedDomains;

    @Override
    public void initialize()
    {
        super.initialize();
        allowedDomains = allowedDomainKeys.stream().
            map( k -> DomainConfigImpl.of( cfgStr( "dns.domain." + k ), cfgStr( "dns.zoneId." + k ) ) ).
            collect( Collectors.toList() );
    }

    @Override
    protected void onNewAdd( final Domain newResource )
    {
        editDomain( newResource, domain -> {
            initDomainStatus( domain );
        } );
    }

    @Override
    public void onUpdate( final Domain oldResource, final Domain newResource )
    {
        editDomain( newResource, domain -> {
            initDomainStatus( domain );

            if ( domain.getDomainStatus().getState() == DomainStatus.State.READY && Objects.equals( oldResource, newResource ) )
            {
                return;
            }

            DomainConfig config = getDomainConfig( domain.getDomainSpec().getHost() );
            if ( config != null )
            {
                // Domain managed by the operator
                syncDnsRecords( config, domain, false );
            }
            else
            {
                // Domain handled by external dns
                lookupDomain( domain );
            }
        } );
    }

    @Override
    public void onDelete( final Domain oldResource, final boolean b )
    {
        DomainConfig config = getDomainConfig( oldResource.getDomainSpec().getHost() );
        if ( config != null )
        {
            syncDnsRecords( config, oldResource, true );
        }
    }

    private void initDomainStatus( final Domain domain )
    {
        DomainStatus status = new DomainStatus().
            withMessage( domain.getDomainStatus().getMessage() ).
            withState( domain.getDomainStatus().getState() ).
            withDomainStatusFields( new DomainStatusFields().
                withPublicIps( new LinkedList<>( lbServiceIpProducer.getIps() ) ).
                withDnsRecordCreated( false ) );

        if ( !domain.getDomainSpec().getDnsRecord() )
        {
            status = status.
                withState( DomainStatus.State.READY ).
                withMessage( "OK" );
        }

        if ( domain.getDomainSpec().getDnsRecord() && status.getState() == DomainStatus.State.PENDING )
        {
            status = status.withMessage( "Waiting for DNS records" );
        }

        domain.withDomainStatus( status );
    }

    private void lookupDomain( final Domain domain )
    {
        // TODO: Handle CNAME
        if ( doh.query( "A", domain.getDomainSpec().getHost() ).answers().size() > 0 )
        {
            domain.getDomainStatus().
                withState( DomainStatus.State.READY ).
                withMessage( "OK" );
        }
    }

    private void syncDnsRecords( final DomainConfig config, final Domain domain, final boolean delete )
    {
        // Check for ips
        List<String> ips = domain.getDomainStatus().getDomainStatusFields().getPublicIps();
        if ( ips.size() == 0 )
        {
            log.warn( "Domain does not have an external IP, not altering records" );
            return;
        }

        // Get current records
        List<DnsRecord> records = dnsRecordService.list( config.zoneId(), domain.getDomainSpec().getHost(), null );

        // Get heritage record
        DnsRecord heritageRecord = getHeritageRecord( records );
        if ( records.size() > 0 && heritageRecord == null )
        {
            log.warn( String.format( "Present heritage record does not match this cluster id for domain '%s'",
                                     domain.getDomainSpec().getHost() ) );
            return;
        }

        List<DnsRecord> aRecords = records.stream().filter( r -> "A".equals( r.type() ) ).collect( Collectors.toList() );

        List<DnsRecord> toAdd = new LinkedList<>();
        List<DnsRecord> toModify = new LinkedList<>();
        List<DnsRecord> toRemove = new LinkedList<>();

        if ( delete )
        {
            // Remove all records on delete
            toRemove.addAll( records );
        }
        else
        {
            // Add heritage record
            if ( heritageRecord == null )
            {
                toAdd.add( ImmutableDnsRecord.builder().
                    zone_id( config.zoneId() ).
                    name( domain.getDomainSpec().getHost() ).
                    ttl( Integer.MAX_VALUE ).
                    type( "TXT" ).
                    content( createHeritageRecord() ).build() );
            }

            // Remove all records that do not have the current IPs the lb has
            aRecords.stream().filter( r -> !ips.contains( r.content() ) ).forEach( toRemove::add );

            // Add all records missing
            List<String> currentRecordIps = aRecords.stream().map( DnsRecord::content ).collect( Collectors.toList() );
            ips.stream().filter( ip -> !currentRecordIps.contains( ip ) ).forEach( ip -> toAdd.add( ImmutableDnsRecord.builder().
                zone_id( config.zoneId() ).
                name( domain.getDomainSpec().getHost() ).
                ttl( domain.getDomainSpec().getDnsTTL() ).
                content( ip ).
                type( "A" ).
                proxied( domain.getDomainSpec().getCdn() ).
                build() ) );

            // Modify records that needed modification
            aRecords.stream().filter( r -> !toRemove.contains( r ) ).forEach( r -> {
                if ( !r.ttl().equals( domain.getDomainSpec().getDnsTTL() ) || r.proxied() != domain.getDomainSpec().getCdn() )
                {
                    toModify.add( ImmutableDnsRecord.builder().from( r ).
                        ttl( domain.getDomainSpec().getDnsTTL() ).
                        proxied( domain.getDomainSpec().getCdn() ).
                        build() );
                }
            } );
        }

        List<Runnable> commands = new LinkedList<>();

        toAdd.stream().forEach( r -> commands.add( dnsRecordService.create( r ) ) );
        toModify.stream().forEach( r -> commands.add( dnsRecordService.update( r ) ) );
        toRemove.stream().forEach( r -> commands.add( dnsRecordService.delete( r ) ) );

        if ( !delete )
        {
            // Update status
            domain.getDomainStatus().
                withState( DomainStatus.State.READY ).
                withMessage( "OK" );
        }

        runnableListExecutor.apply( commands );
    }

    private DnsRecord getHeritageRecord( final List<DnsRecord> records )
    {
        return records.stream().
            filter( r -> "TXT".equals( r.type() ) ).
            filter( r -> createHeritageRecord().equals( r.content() ) ).
            findFirst().
            orElse( null );
    }

    private DomainConfig getDomainConfig( final String host )
    {
        for ( DomainConfig dc : allowedDomains )
        {
            if ( host.endsWith( dc.domain() ) )
            {
                return dc;
            }
        }
        return null;
    }

    private String createHeritageRecord()
    {
        return "heritage=ec-operator,id=" + clusterId;
    }

    private void editDomain( final Domain domain, final Consumer<Domain> c )
    {
        int oldHashSpec = domain.getDomainSpec().hashCode();
        int oldHashStatus = domain.getDomainStatus().hashCode();

        c.accept( domain );

        int newHashSpec = domain.getDomainSpec().hashCode();
        int newHashStatus = domain.getDomainStatus().hashCode();

        if ( oldHashSpec != newHashSpec || oldHashStatus != newHashStatus )
        {
            K8sLogHelper.logDoneable( clients.domain().crdClient().
                withName( domain.getMetadata().getName() ).
                edit().
                withSpec( domain.getDomainSpec() ).
                withStatus( domain.getDomainStatus() ) );
        }
    }
}
