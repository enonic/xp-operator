package com.enonic.ec.kubernetes.operator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableXpVHostApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableXpVHostApplyXpConfig;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableXpVHostDeleteIngress;
import com.enonic.ec.kubernetes.operator.commands.vhosts.helpers.ImmutableMapping;
import com.enonic.ec.kubernetes.operator.commands.vhosts.helpers.Mapping;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostCache;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorVHosts
    extends OperatorStall
{
    private final static Logger log = LoggerFactory.getLogger( OperatorVHosts.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpConfigClientProducer configClientProducer;

    @Inject
    XpVHostCache xpVHostCache;

    @Inject
    XpConfigCache xpConfigCache;

    @ConfigProperty(name = "operator.config.xp.vhosts.file")
    String xp7ConfigFile;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpVHostCache.addWatcher( this::watchVHosts );
    }

    private void watchVHosts( final Watcher.Action action, final String s, final Optional<XpVHostResource> oldVHost,
                              final Optional<XpVHostResource> newVHost )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

        // Get namespace
        String namespace = newVHost.map( m -> m.getMetadata().getNamespace() ).orElse(
            oldVHost.map( m -> m.getMetadata().getNamespace() ).orElse( null ) );

        if ( action == Watcher.Action.DELETED )
        {
            ImmutableXpVHostDeleteIngress.builder().
                defaultClient( defaultClientProducer.client() ).
                resource( oldVHost.get() ).
                build().
                addCommands( commandBuilder );
        }
        else
        {
            ImmutableXpVHostApplyIngress.builder().
                defaultClient( defaultClientProducer.client() ).
                resource( newVHost.get() ).
                build().
                addCommands( commandBuilder );
        }

        // Because multiple vHosts could potentially be deployed at the same time,
        // lets use the stall function to let them accumulate before we update config
        stall( () -> {
            for ( Map.Entry<String, List<Mapping>> e : getNodeMappings( namespace ).entrySet() )
            {
                String node = e.getKey();
                String xp7ConfigName = cfgStrFmt( "operator.config.xp.vhosts.name", node );

                // Find old vHost config for node
                Optional<XpConfigResource> config = xpConfigCache.stream().
                    filter( c -> c.getMetadata().getNamespace().equals( namespace ) ).
                    filter( c -> c.getMetadata().getName().equals( xp7ConfigName ) ).
                    filter( c -> node.equals( c.getSpec().node() ) ).
                    findAny();

                // Create / Update config
                ImmutableXpVHostApplyXpConfig.builder().
                    client( configClientProducer.produce() ).
                    namespace( namespace ).
                    name( xp7ConfigName ).
                    file( xp7ConfigFile ).
                    xpConfigResource( config ).
                    node( node ).
                    mappings( e.getValue() ).
                    build().
                    addCommands( commandBuilder );
            }
            try
            {
                commandBuilder.build().execute();
            }
            catch ( Exception e )
            {
                log.error( "Unable to update apps", e );
            }
        } );
    }

    private Map<String, List<Mapping>> getNodeMappings( String namespace )
    {
        // Get all vHosts in this namespace
        List<XpVHostResource> allVHosts = xpVHostCache.stream().
            filter( a -> a.getMetadata().getNamespace().equals( namespace ) ).
            collect( Collectors.toList() );

        // Get all mappings in one place
        List<Mapping> mappings = new LinkedList<>();
        allVHosts.forEach( v -> v.getSpec().mappings().forEach( m -> mappings.add( ImmutableMapping.builder().
            host( v.getSpec().host() ).
            node( m.node() ).
            source( m.source() ).
            target( m.target() ).
            idProvider( Optional.ofNullable( m.idProvider() ) ).
            build() ) ) );

        // Group mappings by node
        return mappings.stream().collect( Collectors.groupingBy( Mapping::node ) );
    }
}
