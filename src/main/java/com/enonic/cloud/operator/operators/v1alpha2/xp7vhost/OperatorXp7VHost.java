package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.helm.ChartRepository;
import com.enonic.cloud.operator.helm.Helm;
import com.enonic.cloud.operator.helm.commands.ImmutableHelmKubeCmdBuilder;
import com.enonic.cloud.operator.operators.common.OperatorNamespaced;
import com.enonic.cloud.operator.operators.common.ResourceInfoXp7DeploymentDependant;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.commands.ImmutableCommandXpVHostsApplyNodeMappings;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.helpers.MappingBuilder;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.DiffConfigMap;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7ConfigMap;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7VHost;

@SuppressWarnings("WeakerAccess")
@ApplicationScoped
public class OperatorXp7VHost
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7VHost.class );

    @Inject
    Clients clients;

    @Inject
    Caches caches;

    @Inject
    Helm helm;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

    @Inject
    @Named("baseValues")
    Map<String, Object> baseValues;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getVHostCache().addEventListener( this::watchVHosts );
        log.info( "Started listening for Xp7VHost events" );

        caches.getConfigMapCache().addEventListener( this::watchConfigMap );
        log.info( "Started listening for ConfigMap events" );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchVHosts( final String actionId, final Watcher.Action action, final Optional<V1alpha2Xp7VHost> oldResource,
                              final Optional<V1alpha2Xp7VHost> newResource )
    {
        // Get info of event
        Optional<ResourceInfoXp7DeploymentDependant<V1alpha2Xp7VHost, DiffXp7VHost>> i =
            getInfo( action, () -> ImmutableInfoXp7VHost.builder().
                caches( caches ).
                oldResource( oldResource ).
                newResource( newResource ).
                build() );

        i.ifPresent( info -> runCommands( actionId, ( commandBuilder ) -> {
            if ( info.resourceBeingRestoredFromBackup() )
            {
                // This is a backup restore, just ignore
                return;
            }

            // Create ingress independent of config
            ImmutableHelmKubeCmdBuilder.builder().
                clients( clients ).
                helm( helm ).
                chart( chartRepository.get( "v1alpha2/xp7vhost" ) ).
                namespace( info.namespace() ).
                valueBuilder( ImmutableXp7VHostValues.builder().
                    baseValues( baseValues ).
                    info( info ).
                    build() ).
                build().
                addCommands( commandBuilder );

            // Update config
            ImmutableCommandXpVHostsApplyNodeMappings.builder().
                caches( caches ).
                clients( clients ).
                info( info ).
                nodeMappings( MappingBuilder.getNodeMappings( caches, info ) ).
                build().
                addCommands( commandBuilder );
        } ) );
    }

    // This watcher is to make sure new node config maps get the correct vhost configuration
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchConfigMap( final String actionId, final Watcher.Action action, final Optional<ConfigMap> oldResource,
                                 final Optional<ConfigMap> newResource )
    {
        // We only care about new ConfigMaps
        if ( action != Watcher.Action.ADDED )
        {
            return;
        }

        // Give other process some slack to finish its thing
        try
        {
            Thread.sleep( 2000L );
        }
        catch ( InterruptedException e )
        {
            // Ignore
        }

        // Get info
        Optional<ResourceInfoXp7DeploymentDependant<ConfigMap, DiffConfigMap>> i =
            getInfo( action, () -> ImmutableInfoXp7ConfigMap.builder().
                caches( caches ).
                oldResource( oldResource ).
                newResource( newResource ).
                build() );

        // Update config
        i.ifPresent( info -> runCommands( actionId, ( commandBuilder ) -> ImmutableCommandXpVHostsApplyNodeMappings.builder().
            caches( caches ).
            clients( clients ).
            info( info ).
            nodeMappings( MappingBuilder.getNodeMappings( caches, info ) ).
            build().
            addCommands( commandBuilder ) ) );
    }
}
