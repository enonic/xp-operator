package com.enonic.ec.kubernetes.dns;

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

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.cache.IngressCache;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.dns.cloudflare.DnsRecordService;
import com.enonic.ec.kubernetes.dns.commands.ImmutableDnsApplyIngress;
import com.enonic.ec.kubernetes.dns.model.DiffDnsIngress;
import com.enonic.ec.kubernetes.dns.model.Domain;
import com.enonic.ec.kubernetes.dns.model.ImmutableDiffDnsIngress;
import com.enonic.ec.kubernetes.dns.model.ImmutableDnsIngress;
import com.enonic.ec.kubernetes.dns.model.ImmutableDomain;


@ApplicationScoped
public class OperatorDns
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( OperatorDns.class );

    @Inject
    IngressCache ingressCache;

    @RestClient
    @Inject
    DnsRecordService dnsRecordService;

    @ConfigProperty(name = "dns.enabled")
    Boolean enabled;

    @ConfigProperty(name = "dns.allowedDomains.keys")
    List<String> allowedDomainKeys;

    List<Domain> allowedDomains;

    void onStartup( @Observes StartupEvent _ev )
    {
        allowedDomains = allowedDomainKeys.stream().map( k -> ImmutableDomain.builder().
            zoneId( cfgStr( "dns.zoneId." + k ) ).
            domain( cfgStr( "dns.domain." + k ) ).
            build() ).collect( Collectors.toList() );

        ingressCache.addWatcher( this::watchIngress );
    }

    private void watchIngress( final Watcher.Action action, final String s, final Optional<Ingress> oldIngress,
                               final Optional<Ingress> newIngress )
    {
        if ( !enabled )
        {
            return;
        }

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
            ImmutableDnsApplyIngress.builder().
                dnsRecordService( dnsRecordService ).
                diff( diff ).
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
            e.printStackTrace(); // TODO: Do something
        }
    }
}
