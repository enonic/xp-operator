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
import com.enonic.cloud.apis.doh.service.DohAnswer;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatus;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

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
    LbServiceIpProducer ips;

    @Inject
    Searchers searchers;

    @Inject
    @Named("clusterId")
    String clusterId;

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
        handle( newResource );
    }

    @Override
    public void onUpdate( final Domain oldResource, final Domain newResource )
    {
        // If domain is ready and spec has not changed, just return
        if ( newResource.getDomainStatus().getState() == DomainStatus.State.READY &&
            Objects.equals( oldResource.getDomainSpec(), newResource.getDomainSpec() ) )
        {
            return;
        }

        handle( newResource );
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

    private void handle( Domain domainToChange )
    {
        editDomain( domainToChange, domain -> {
            DomainConfig config = getDomainConfig( domain.getDomainSpec().getHost() );
            if ( config != null )
            {
                // Domain managed by the operator
                syncDnsRecords( config, domain, !domain.getDomainSpec().getDnsRecord() );
            }
            else
            {
                // Domain handled by external dns
                lookupDomain( domain );
            }
        } );
    }

    private void lookupDomain( final Domain domain )
    {
        // We do not expect dns records
        if ( !domain.getDomainSpec().getDnsRecord() )
        {
            updateStatus( domain, DomainStatus.State.READY, "OK", false );
            return;
        }

        List<String> ourIps = ips.get();

        // Try to find a records
        List<DohAnswer> aRecords = doh.query( "A", domain.getDomainSpec().getHost() ).answers();
        if ( aRecords.size() > 0 )
        {
            List<String> matches = new LinkedList<>();
            for ( DohAnswer r : aRecords )
            {
                for ( String ip : ourIps )
                {
                    if ( ip.equals( r.data() ) )
                    {
                        matches.add( r.data() );
                    }
                }
            }
            if ( matches.size() == ips.get().size() )
            {
                updateStatus( domain, DomainStatus.State.READY, "External A records match", true );
            }
            else
            {
                updateStatus( domain, DomainStatus.State.ERROR, "External A records do not match", false );
            }
            return;
        }

        List<DohAnswer> cnameRecords = doh.query( "CNAME", domain.getDomainSpec().getHost() ).answers();
        if ( cnameRecords.size() == 1 )
        {
            String cnameDomain = cnameRecords.get( 0 ).data();
            boolean realDomainExists = searchers.domain().
                query().
                filter( d -> d.getDomainSpec().getHost().equals( cnameDomain ) ).list().size() == 1;
            if ( realDomainExists )
            {
                updateStatus( domain, DomainStatus.State.READY, "External CNAME record matches", true );
            }
            else
            {
                updateStatus( domain, DomainStatus.State.ERROR, "External CNAME record does not match", true );
            }
        }

        // No records were found
        updateStatus( domain, DomainStatus.State.ERROR, "No External record found", false );
    }

    private void syncDnsRecords( final DomainConfig config, final Domain domain, final boolean delete )
    {
        // Check for ips
        List<String> ips = domain.getDomainStatus().getDomainStatusFields().getPublicIps();
        if ( ips.size() == 0 )
        {
            log.warn( "Domain does not have an external IP, not altering records" );
            updateStatus( domain, DomainStatus.State.ERROR, "No external IP found", false );
            return;
        }

        // Get current records
        List<DnsRecord> records = dnsRecordService.list( config.zoneId(), domain.getDomainSpec().getHost(), null );

        // If we are not suppose to create records
        if ( records.size() == 0 && !domain.getDomainSpec().getDnsRecord() )
        {
            updateStatus( domain, DomainStatus.State.READY, "OK", false );
            return;
        }

        // Get heritage record
        DnsRecord heritageRecord = getHeritageRecord( records );
        if ( records.size() > 0 && heritageRecord == null )
        {
            log.warn( String.format( "Present heritage record does not match this cluster id for domain '%s'",
                                     domain.getDomainSpec().getHost() ) );
            updateStatus( domain, DomainStatus.State.ERROR, "Heritage record mismatch", false );
            return;
        }

        // Collect A records
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

        // Collect commands
        List<Runnable> commands = new LinkedList<>();
        toAdd.stream().forEach( r -> commands.add( dnsRecordService.create( r ) ) );
        toModify.stream().forEach( r -> commands.add( dnsRecordService.update( r ) ) );
        toRemove.stream().forEach( r -> commands.add( dnsRecordService.delete( r ) ) );

        if ( !delete )
        {
            updateStatus( domain, DomainStatus.State.READY, "OK", true );
        }

        try
        {
            runnableListExecutor.apply( commands );
        }
        catch ( Exception e )
        {
            updateStatus( domain, DomainStatus.State.ERROR, "Faild updating records, see operator logs", false );
            log.error( "Failed calling CF: " + e.getMessage() );
        }
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

    private void updateStatus( Domain domain, DomainStatus.State state, String message, boolean dnsRecordCreated )
    {
        domain.getDomainStatus().
            withState( state ).
            withMessage( message ).
            withDomainStatusFields( domain.getDomainStatus().getDomainStatusFields().
                withDnsRecordCreated( dnsRecordCreated ) );
    }
}
