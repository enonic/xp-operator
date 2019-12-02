package com.enonic.ec.kubernetes.operator;

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
import com.enonic.ec.kubernetes.dns.cloudflare.DnsRecords;
import com.enonic.ec.kubernetes.operator.dns.AllowedDomain;
import com.enonic.ec.kubernetes.operator.dns.DiffDnsIngress;
import com.enonic.ec.kubernetes.operator.dns.DnsIngress;
import com.enonic.ec.kubernetes.operator.dns.ImmutableAllowedDomain;
import com.enonic.ec.kubernetes.operator.dns.ImmutableDiffDnsIngress;
import com.enonic.ec.kubernetes.operator.dns.ImmutableDnsIngress;
import com.enonic.ec.kubernetes.operator.dns.commands.ImmutableDnsApplyIngress;
import com.enonic.ec.kubernetes.operator.dns.commands.ImmutableDnsDeleteIngress;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorDns
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( OperatorDns.class );

    @Inject
    IngressCache ingressCache;

    @RestClient
    @Inject
    DnsRecords dnsRecords;

    @ConfigProperty(name = "dns.allowedDomains.keys", defaultValue = "")
    List<String> allowedDomainKeys;

    List<AllowedDomain> allowedDomains;

    void onStartup( @Observes StartupEvent _ev )
    {
        allowedDomains = allowedDomainKeys.stream().map( k -> ImmutableAllowedDomain.builder().
            zoneId( cfgStr( "dns.zoneId." + k ) ).
            domain( cfgStr( "dns.domain." + k ) ).
            build() ).collect( Collectors.toList() );

        ingressCache.addWatcher( this::watchIngress );
    }

    private void watchIngress( final Watcher.Action action, final String s, final Optional<Ingress> oldIngress,
                               final Optional<Ingress> newIngress )
    {
        DiffDnsIngress diff = ImmutableDiffDnsIngress.builder().
            oldValue( Optional.ofNullable( oldIngress.map( o -> ImmutableDnsIngress.builder().
                ingress( o ).
                allowedDomains( allowedDomains ).
                build() ).
                orElse( null ) ) ).
            newValue( Optional.ofNullable( newIngress.map( n -> ImmutableDnsIngress.builder().
                ingress( n ).
                allowedDomains( allowedDomains ).
                build() ).
                orElse( null ) ) ).
            build();

        try
        {
            ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

            boolean addOrModify = diff.shouldAddOrModify();
            addOrModify = addOrModify && isOk( diff.newValue().get() );
            addOrModify = addOrModify && isOk( diff.newValue().get() );
            addOrModify = addOrModify && diff.ipChanged();
            addOrModify = addOrModify && diff.newValue().get().ips().size() > 0;
            boolean remove = diff.shouldRemove() && isOk( diff.oldValue().get() );

            if ( addOrModify )
            {
                ImmutableDnsApplyIngress.builder().
                    dnsRecords( dnsRecords ).
                    dnsIngress( diff.newValue().get() ).
                    build().
                    addCommands( commandBuilder );
            }

            if ( remove )
            {
                ImmutableDnsDeleteIngress.builder().
                    dnsRecords( dnsRecords ).
                    dnsIngress( diff.oldValue().get() ).
                    build().
                    addCommands( commandBuilder );
            }

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

    private boolean isOk( DnsIngress dns )
    {
        return dns.allowedDomain().isPresent() && dns.subDomain().isPresent();
    }
}
