package com.enonic.cloud.operator.domain;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Service;

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

import static com.enonic.cloud.common.Configuration.cfgHasKey;
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
        handle( newResource, false, false );
    }

    @Override
    public void onUpdate( final Domain oldResource, final Domain newResource )
    {
        boolean cdnChange = oldResource.getDomainSpec().getCdn() != newResource.getDomainSpec().getCdn();
        boolean ttlChange = !oldResource.getDomainSpec().getDnsTTL().equals( newResource.getDomainSpec().getDnsTTL() );
        boolean dnsRecordChange = oldResource.getDomainSpec().getDnsRecord() != newResource.getDomainSpec().getDnsRecord();
        handle( newResource, !newResource.getDomainSpec().getDnsRecord(), cdnChange || ttlChange || dnsRecordChange );
    }

    @Override
    public void onDelete( final Domain oldResource, final boolean b )
    {
        handle( oldResource, true, false );
    }

    private Void handle( final Domain domain, final boolean delete, final boolean forceDnsRecordChange )
    {
        // Check if domain has been assigned public IPs
        if ( domain.getDomainStatus().getDomainStatusFields().getPublicIps().size() == 0 && !delete )
        {
            return addPublicIps( domain );
        }

        DomainConfig config = getDomainConfig( domain.getDomainSpec().getHost() );

        if ( config == null && !domain.getDomainStatus().getDomainStatusFields().getDnsRecordCreated() )
        {
            log.warn( String.format( "Domain '%s' is not in allowed domains list", domain.getDomainSpec().getHost() ) );
            return updateDnsStatus( domain );
        }

        if ( domain.getDomainStatus().getState() == DomainStatus.State.READY && !delete && !forceDnsRecordChange )
        {
            return null;
        }

        List<DnsRecord> records = dnsRecordService.list( config.zoneId(), domain.getDomainSpec().getHost(), null );
        if ( records.size() == 0 && delete )
        {
            // No records to delete, ignore
            return null;
        }

        return syncDnsRecords( config, domain, records, delete );
    }

    private Void addPublicIps( final Domain domain )
    {
        Set<String> ips = new HashSet<>();

        if ( cfgHasKey( "operator.dns.lb.staticIp" ) )
        {
            ips.add( cfgStr( "operator.dns.lb.staticIp" ) );
        }
        else
        {
            Service lbService = clients.k8s().services().
                inNamespace( cfgStr( "operator.dns.lb.service.namespace" ) ).
                withName( cfgStr( "operator.dns.lb.service.name" ) ).
                get();

            if ( lbService == null )
            {
                log.warn( "Loadbalancer service not found" );
                return null;
            }

            if ( lbService.getStatus() != null && lbService.getStatus().getLoadBalancer() != null &&
                lbService.getStatus().getLoadBalancer().getIngress() != null )
            {
                for ( LoadBalancerIngress ingress : lbService.getStatus().getLoadBalancer().getIngress() )
                {
                    ips.add( ingress.getIp() );
                }
            }
        }

        if ( ips.size() != 0 )
        {
            DomainStatus status = new DomainStatus().
                withDomainStatusFields( new DomainStatusFields().
                    withPublicIps( new LinkedList<>( ips ) ).
                    withDnsRecordCreated( false ) );

            if ( domain.getDomainSpec().getDnsRecord() )
            {
                status = status.
                    withState( DomainStatus.State.PENDING ).
                    withMessage( "Waiting for dns records" );
            }
            else
            {
                status = status.
                    withState( DomainStatus.State.READY ).
                    withMessage( "OK" );
            }

            K8sLogHelper.logDoneable( clients.domain().crdClient().
                withName( domain.getMetadata().getName() ).
                edit().
                withStatus( status ) );
        }

        return null;
    }

    private Void updateDnsStatus( final Domain domain )
    {
        // TODO: Handle CNAME
        if ( doh.query( "A", domain.getDomainSpec().getHost() ).answers().size() > 0 )
        {
            K8sLogHelper.logDoneable( clients.domain().crdClient().
                withName( domain.getMetadata().getName() ).
                edit().
                withStatus( new DomainStatus().
                    withState( DomainStatus.State.READY ).
                    withMessage( "OK" ).
                    withDomainStatusFields( new DomainStatusFields().
                        withPublicIps( domain.getDomainStatus().getDomainStatusFields().getPublicIps() ).
                        withDnsRecordCreated( true ) ) ) );
        }
        return null;
    }

    private Void syncDnsRecords( final DomainConfig config, final Domain domain, final List<DnsRecord> records, final boolean delete )
    {

        DnsRecord heritageRecord = getHeritageRecord( records );
        if ( records.size() > 0 && heritageRecord == null )
        {
            log.warn( String.format( "Present heritage record does not match this cluster id", domain.getDomainSpec().getHost() ) );
            return null;
        }

        List<String> ips = domain.getDomainStatus().getDomainStatusFields().getPublicIps();

        if ( ips.size() == 0 )
        {
            log.warn( "Domain does not have an external IP" );
            return null;
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
            // Add heritage record on deletion
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
            commands.add( () -> K8sLogHelper.logDoneable( clients.domain().crdClient().
                withName( domain.getMetadata().getName() ).
                edit().
                withStatus( new DomainStatus().
                    withState( DomainStatus.State.READY ).
                    withMessage( "OK" ).
                    withDomainStatusFields( new DomainStatusFields().
                        withPublicIps( new LinkedList<>( ips ) ).
                        withDnsRecordCreated( true ) ) ) ) );
        }

        runnableListExecutor.apply( commands );
        return null;
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
}
