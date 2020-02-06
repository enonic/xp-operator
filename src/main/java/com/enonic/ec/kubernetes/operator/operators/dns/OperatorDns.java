package com.enonic.ec.kubernetes.operator.operators.dns;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.DnsRecordService;
import com.enonic.ec.kubernetes.operator.operators.dns.commands.ImmutableDnsApplyIngress;
import com.enonic.ec.kubernetes.operator.operators.dns.model.DiffDnsIngress;
import com.enonic.ec.kubernetes.operator.operators.dns.model.Domain;
import com.enonic.ec.kubernetes.operator.operators.dns.model.ImmutableDiffDnsIngress;
import com.enonic.ec.kubernetes.operator.operators.dns.model.ImmutableDnsIngress;
import com.enonic.ec.kubernetes.operator.operators.dns.model.ImmutableDomain;


@ApplicationScoped
public class OperatorDns
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( OperatorDns.class );

    @Inject
    Caches caches;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @RestClient
    @Inject
    DnsRecordService dnsRecordService;

    @ConfigProperty(name = "dns.enabled")
    Boolean enabled;

    @ConfigProperty(name = "dns.allowedDomains.keys")
    List<String> allowedDomainKeys;

    private List<Domain> allowedDomains;

    void onStartup( @Observes StartupEvent _ev )
    {
        if ( !enabled )
        {
            return;
        }
        allowedDomains = allowedDomainKeys.stream().map( k -> ImmutableDomain.builder().
            zoneId( cfgStr( "dns.zoneId." + k ) ).
            domain( cfgStr( "dns.domain." + k ) ).
            build() ).collect( Collectors.toList() );

        caches.getIngressCache().addWatcher( this::watchIngress );
    }

    @SuppressWarnings({"UnstableApiUsage", "OptionalUsedAsFieldOrParameterType"})
    private void watchIngress( final Watcher.Action action, final String s, final Optional<Ingress> oldIngress,
                               final Optional<Ingress> newIngress )
    {
        DiffDnsIngress diff = ImmutableDiffDnsIngress.builder().
            oldValue( oldIngress.map( o -> ImmutableDnsIngress.builder().
                ingress( o ).
                allowedDomains( allowedDomains ).
                build() ) ).
            newValue( newIngress.map( n -> ImmutableDnsIngress.builder().
                ingress( n ).
                allowedDomains( allowedDomains ).
                build() ) ).
            build();

        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

        try
        {
            String ingressName = newIngress.
                map( r -> r.getMetadata().getName() ).
                orElse( oldIngress.map( r -> r.getMetadata().getName() ).orElse( null ) );

            // Note: By changing this dnsId older managed records will stop working
            String dnsId = Hashing.sha512().hashString( ingressName, Charsets.UTF_8 ).toString().substring( 0, 16 );

            ImmutableDnsApplyIngress.builder().
                dnsRecordService( dnsRecordService ).
                diff( diff ).
                dnsId( dnsId ).
                build().
                addCommands( commandBuilder );
            commandBuilder.build().execute();
        }
        catch ( WebApplicationException e )
        {
            log.error( "Unable set DNS records: " + e.getMessage() + " " + e.getResponse().readEntity( String.class ) );
        }
        catch ( Exception e )
        {
            log.error( "Exception when setting DNS records", e );
        }
    }
}
