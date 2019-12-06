package com.enonic.ec.kubernetes.operator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ConfigMapCache;
import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.config.ImmutableXpConfigApply;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorConfig
    extends OperatorStall
{
    private final static Logger log = LoggerFactory.getLogger( OperatorConfig.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpConfigCache xpConfigCache;

    @Inject
    ConfigMapCache configMapCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpConfigCache.addWatcher( this::watchXpConfig );
    }

    private void watchXpConfig( final Watcher.Action action, final String s, final Optional<XpConfigResource> oldXpConfig,
                                final Optional<XpConfigResource> newXpConfig )
    {
        // Because multiple configs could potentially be deployed at the same time,
        // lets use the stall function to let them accumulate before we update config
        stall( () -> {
            // Get config resource
            XpConfigResource configResource = action == Watcher.Action.DELETED ? oldXpConfig.get() : newXpConfig.get();

            // Get relevant node config maps
            List<ConfigMap> configMaps = getRelevantConfigMaps( configResource );

            // Update config maps
            for ( ConfigMap configMap : configMaps )
            {
                // Get all relevant XpConfig for this config map
                List<XpConfigResource> allXpConfigs = getRelevantXpConfig( configMap );

                // Update
                apply( configMap, allXpConfigs );
            }
        } );
    }

    private synchronized void apply( final ConfigMap configMap, final List<XpConfigResource> xpConfigs )
    {
        try
        {
            ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();
            ImmutableXpConfigApply.builder().
                defaultClient( defaultClientProducer.client() ).
                configMap( configMap ).
                xpConfigResources( xpConfigs ).build().
                addCommands( commandBuilder );
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            log.error( "Unable to update configMap", e );
        }
    }

    private List<ConfigMap> getRelevantConfigMaps( final XpConfigResource configResource )
    {
        return configMapCache.stream().
            // Filter by namespace
                filter( c -> c.getMetadata().getNamespace().equals( configResource.getMetadata().getNamespace() ) ).
            // Filter by XpConfig node if any
                filter( c -> configResource.getSpec().node() == null ||
                configResource.getSpec().node().equals( c.getMetadata().getName() ) ).collect( Collectors.toList() );
    }

    private List<XpConfigResource> getRelevantXpConfig( final ConfigMap configMap )
    {
        return xpConfigCache.stream().
            // Filter by namespace
                filter( c -> c.getMetadata().getNamespace().equals( configMap.getMetadata().getNamespace() ) ).
            // Filter by XpConfig node if any
                filter( c -> c.getSpec().node() == null || configMap.getMetadata().getName().equals( c.getSpec().node() ) ).
                collect( Collectors.toList() );
    }
}
