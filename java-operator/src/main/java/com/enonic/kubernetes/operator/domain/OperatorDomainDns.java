package com.enonic.kubernetes.operator.domain;

import com.enonic.kubernetes.apis.cloudflare.DnsRecordServiceWrapper;
import com.enonic.kubernetes.apis.cloudflare.service.model.DnsRecord;
import com.enonic.kubernetes.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.kubernetes.apis.doh.DohServiceWrapper;
import com.enonic.kubernetes.apis.doh.service.DohAnswer;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.domain.DomainStatus;
import com.enonic.kubernetes.common.functions.RunnableListExecutor;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;

import io.quarkus.runtime.StartupEvent;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import static com.enonic.kubernetes.common.Configuration.cfgIfBool;
import static com.enonic.kubernetes.common.Configuration.cfgStr;

/**
 * This operator class creates/updates/deletes DNS records in cloudflare based on Domains
 */
@ApplicationScoped
public class OperatorDomainDns
    extends InformerEventHandler<Domain>
{
    private final Logger log = LoggerFactory.getLogger( OperatorDomainDns.class );

    @Inject
    Clients clients;

    @Inject
    Informers informers;

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

    @ConfigProperty(name = "dns.cname")
    String cnameDomain;

    void onStart( @Observes StartupEvent ev )
    {
        allowedDomains = allowedDomainKeys.stream()
            .map( k -> DomainConfigImpl.of( cfgStr( "dns.domain." + k ), cfgStr( "dns.zoneId." + k ) ) )
            .collect( Collectors.toList() );

        cfgIfBool( "dns.enabled", () -> listen( informers.domainInformer() ) );
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
        if ( newResource.getStatus().getState() == DomainStatus.State.READY &&
            Objects.equals( oldResource.getSpec(), newResource.getSpec() ) )
        {
            return;
        }

        handle( newResource );
    }

    @Override
    public void onDelete( final Domain oldResource, final boolean b )
    {
        DomainConfig config = getDomainConfig( oldResource.getSpec().getHost() );
        if ( config != null )
        {
            syncDnsRecords( config, oldResource, true );
        }
    }

    private void handle( Domain domainToChange )
    {
        editDomain( domainToChange, domain -> {
            DomainConfig config = getDomainConfig( domain.getSpec().getHost() );
            if ( config != null )
            {
                // Domain managed by the operator
                syncDnsRecords( config, domain, !domain.getSpec().getDnsRecord() );
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
        if ( !domain.getSpec().getDnsRecord() )
        {
            updateStatus( domain, DomainStatus.State.READY, "OK", false );
            return;
        }

        List<String> ourIps = ips.get();

        // Try to find a records
        List<DohAnswer> aRecords = doh.query( "A", domain.getSpec().getHost() ).answers();
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

        List<DohAnswer> cnameRecords = doh.query( "CNAME", domain.getSpec().getHost() ).answers();
        if ( cnameRecords.size() == 1 )
        {
            String cnameDomain = cnameRecords.get( 0 ).data();
            boolean realDomainExists = searchers.domain().stream().filter( d -> d.getSpec().getHost().equals( cnameDomain ) ).count() == 1;
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
        List<String> ips = domain.getStatus().getDomainStatusFields().getPublicIps();
        if ( ips.size() == 0 )
        {
            log.warn( "Domain does not have an external IP, not altering records" );
            updateStatus( domain, DomainStatus.State.ERROR, "No external IP found", false );
            return;
        }

        // Get current records
        List<DnsRecord> records = dnsRecordService.list( config.zoneId(), domain.getSpec().getHost(), null );

        // If we are not supposed to create records
        if ( records.size() == 0 && !domain.getSpec().getDnsRecord() )
        {
            updateStatus( domain, DomainStatus.State.READY, "OK", false );
            return;
        }

        final DnsRecordManager dnsRecordManager = new DnsRecordManager( dnsRecordService, runnableListExecutor, config, domain );

        // Get heritage record
        final DnsRecord heritageRecord = dnsRecordManager.getHeritageRecord( records, clusterId );

        if ( records.size() > 0 && heritageRecord == null )
        {
            log.warn(
                String.format( "Present heritage record does not match this cluster id for domain '%s'", domain.getSpec().getHost() ) );
            updateStatus( domain, DomainStatus.State.ERROR, "Heritage record mismatch", false );
            return;
        }

        String errorMessage;

        if ( delete )
        {
            errorMessage = dnsRecordManager.deleteRecords( records );
        }
        else
        {
            // sync records
            errorMessage = cnameDomain == null
                ? dnsRecordManager.syncRecords( records, ips, "A", clusterId )
                : dnsRecordManager.syncRecords( records, List.of( cnameDomain ), "CNAME", clusterId );
        }

        final DomainStatus.State newState = errorMessage.isBlank() ? DomainStatus.State.READY : DomainStatus.State.ERROR;
        final String statusMessage = errorMessage.isBlank() ? "OK" : errorMessage;

        updateStatus( domain, newState, statusMessage, errorMessage.isBlank() );
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

    private void editDomain( final Domain domain, final Consumer<Domain> c )
    {
        int oldHashSpec = domain.getSpec().hashCode();
        int oldHashStatus = domain.getStatus().hashCode();

        c.accept( domain );

        int newHashSpec = domain.getSpec().hashCode();
        int newHashStatus = domain.getStatus().hashCode();

        if ( oldHashSpec != newHashSpec || oldHashStatus != newHashStatus )
        {
            K8sLogHelper.logEdit( clients.domain().withName( domain.getMetadata().getName() ),
                                  d -> d.withSpec( domain.getSpec() ).withStatus( domain.getStatus() ) );
        }
    }

    private void updateStatus( Domain domain, DomainStatus.State state, String message, boolean dnsRecordCreated )
    {
        domain.getStatus()
            .withState( state )
            .withMessage( message )
            .withDomainStatusFields( domain.getStatus().getDomainStatusFields().withDnsRecordCreated( dnsRecordCreated ) );
    }
}
