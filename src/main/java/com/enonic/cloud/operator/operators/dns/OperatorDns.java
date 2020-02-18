package com.enonic.cloud.operator.operators.dns;

import java.util.List;
import java.util.Objects;
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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.operators.common.Operator;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.dns.cloudflare.DnsRecordService;
import com.enonic.cloud.operator.operators.dns.commands.ImmutableDnsApplyIngress;
import com.enonic.cloud.operator.operators.dns.model.DiffDnsIngress;
import com.enonic.cloud.operator.operators.dns.model.Domain;
import com.enonic.cloud.operator.operators.dns.model.ImmutableDiffDnsIngress;
import com.enonic.cloud.operator.operators.dns.model.ImmutableDnsIngress;
import com.enonic.cloud.operator.operators.dns.model.ImmutableDomain;


@SuppressWarnings("WeakerAccess")
@ApplicationScoped
public class OperatorDns
    extends Operator
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

        caches.getIngressCache().addEventListener( this::watchIngress );
        log.info( "Started listening for Ingress events" );
    }

    @SuppressWarnings({"UnstableApiUsage", "OptionalUsedAsFieldOrParameterType"})
    private void watchIngress( final String actionId, final Watcher.Action action, final Optional<Ingress> oldIngress, final Optional<Ingress> newIngress )
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

        HasMetadata resource = Objects.requireNonNull( newIngress.orElse( oldIngress.orElse( null ) ) );

        String ingressName = resource.getMetadata().getName();

        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( actionId );

        try
        {
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
