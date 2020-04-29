package com.enonic.cloud.operator.operators.v1alpha2.xp7config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.operators.common.OperatorNamespaced;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.cloud.operator.operators.common.ResourceInfoXp7DeploymentDependant;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.common.queues.OperatorChangeQueues;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.DiffConfigMap;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.ImmutableInfoConfigMap;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.ImmutableInfoXp7Config;


@SuppressWarnings("WeakerAccess")
@ApplicationScoped
public class OperatorXp7Config
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7Config.class );

    @Inject
    Clients clients;

    @Inject
    OperatorChangeQueues changeQueues;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getConfigCache().addEventListener( this::watchXpConfig );
        log.info( "Started listening for Xp7Config events" );

        caches.getConfigMapCache().addEventListener( this::watchConfigMap );
        log.info( "Started listening for ConfigMap events" );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchConfigMap( final String actionId, final Watcher.Action action, final Optional<ConfigMap> oldResource,
                                 final Optional<ConfigMap> newResource )
    {
        // If this is a delete event, ignore
        if ( action == Watcher.Action.DELETED )
        {
            return;
        }

        // Get info of event
        Optional<ResourceInfoNamespaced<ConfigMap, DiffConfigMap>> i = getInfo( action, () -> ImmutableInfoConfigMap.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {
            if ( isNamespaceBeingTerminated( info ) )
            {
                // Everything is about to be deleted, just ignore
                return;
            }

            if ( info.resourceBeingRestoredFromBackup() )
            {
                // This is a backup/restore event, just ignore
                return;
            }

            // Sync the config map with Xp7Config
            if ( filterNodeGroupConfigMaps( info.resource() ) )
            {
                changeQueues.getConfigMapResourceChangeQueue().
                    enqueue( actionId, ImmutableConfigMapAggregator.builder().
                        caches( caches ).
                        clients( clients ).
                        metadata( info.resource().getMetadata() ).
                        build() );
            }
        } );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchXpConfig( final String actionId, final Watcher.Action action, final Optional<V1alpha2Xp7Config> oldResource,
                                final Optional<V1alpha2Xp7Config> newResource )
    {
        // Get info of event
        Optional<ResourceInfoXp7DeploymentDependant<V1alpha2Xp7Config, DiffXp7Config>> i =
            getInfo( action, () -> ImmutableInfoXp7Config.builder().
                caches( caches ).
                oldResource( oldResource ).
                newResource( newResource ).
                build() );

        i.ifPresent( info -> {
            if ( info.resourceBeingRestoredFromBackup() )
            {
                // This is a backup/restore event, just ignore
                return;
            }

            // Update all nodeGroup config maps in namespace
            caches.getConfigMapCache().
                getByNamespace( info.namespace() ).
                filter( this::filterNodeGroupConfigMaps ).
                forEach( config -> changeQueues.getConfigMapResourceChangeQueue().
                    enqueue( actionId, ImmutableConfigMapAggregator.builder().
                        caches( caches ).
                        clients( clients ).
                        metadata( config.getMetadata() ).
                        build() ) );
        } );
    }

    private boolean filterNodeGroupConfigMaps( final ConfigMap configMap )
    {
        return configMap.getMetadata().getLabels() != null && configMap.getMetadata().getLabels().containsKey( "nodeGroup" );
    }
}
