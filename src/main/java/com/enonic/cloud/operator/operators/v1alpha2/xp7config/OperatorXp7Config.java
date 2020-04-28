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
import com.enonic.cloud.operator.operators.common.ResourceInfoXp7DeploymentDependant;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.common.queues.OperatorChangeQueues;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;
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
    Caches caches;

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
        ConfigMap resource = newResource.orElseGet( oldResource::get );
        if ( filterNodeGroupConfigMaps( resource ) )
        {
            changeQueues.getConfigMapResourceChangeQueue().
                enqueue( actionId, ImmutableConfigMapAggregator.builder().
                    caches( caches ).
                    clients( clients ).
                    metadata( resource.getMetadata() ).
                    build() );
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchXpConfig( final String actionId, final Watcher.Action action, final Optional<V1alpha2Xp7Config> oldResource,
                                final Optional<V1alpha2Xp7Config> newResource )
    {
        Optional<ResourceInfoXp7DeploymentDependant<V1alpha2Xp7Config, DiffXp7Config>> i =
            getInfo( action, () -> ImmutableInfoXp7Config.builder().
                caches( caches ).
                oldResource( oldResource ).
                newResource( newResource ).
                build() );

        i.ifPresent( info -> caches.getConfigMapCache().
            getByNamespace( info.namespace() ).
            filter( this::filterNodeGroupConfigMaps ).
            forEach( config -> changeQueues.getConfigMapResourceChangeQueue().
                enqueue( actionId, ImmutableConfigMapAggregator.builder().
                    caches( caches ).
                    clients( clients ).
                    metadata( config.getMetadata() ).
                    build() ) ) );
    }

    private boolean filterNodeGroupConfigMaps( final ConfigMap configMap )
    {
        return configMap.getMetadata().getLabels() != null && configMap.getMetadata().getLabels().containsKey( "nodeGroup" );
    }
}
