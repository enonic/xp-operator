package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ObjectMeta;
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
import com.enonic.cloud.operator.operators.common.queues.OperatorChangeQueues;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7VHost;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.common.Configuration.cfgStrFmt;

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
    OperatorChangeQueues changeQueues;

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

            Set<String> affectedNodeGroups = new HashSet<>();
            info.oldResource().ifPresent( r -> r.getSpec().mappings().forEach( m -> affectedNodeGroups.add( m.nodeGroup() ) ) );
            info.newResource().ifPresent( r -> r.getSpec().mappings().forEach( m -> affectedNodeGroups.add( m.nodeGroup() ) ) );

            if ( affectedNodeGroups.contains( cfgStr( "operator.helm.charts.Values.allNodesKey" ) ) )
            {
                info.xpDeploymentResource().getSpec().nodeGroups().keySet().
                    forEach( nodeGroup -> updateNodeGroup( actionId, info, nodeGroup ) );
            }
            else
            {
                affectedNodeGroups.
                    forEach( nodeGroup -> updateNodeGroup( actionId, info, nodeGroup ) );
            }
        } ) );
    }

    private void updateNodeGroup( final String actionId, final ResourceInfoXp7DeploymentDependant<V1alpha2Xp7VHost, DiffXp7VHost> info,
                                  final String nodeGroup )
    {
        changeQueues.getV1alpha2Xp7ConfigResourceChangeQueue().
            enqueue( actionId, ImmutableXp7VHostAggregator.builder().
                clients( clients ).
                caches( caches ).
                metadata( createMetadata( info, nodeGroup ) ).
                nodeGroup( nodeGroup ).
                build() );
    }

    private ObjectMeta createMetadata( final ResourceInfoXp7DeploymentDependant<V1alpha2Xp7VHost, DiffXp7VHost> info,
                                       final String nodeGroup )
    {
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace( info.namespace() );
        meta.setName( cfgStrFmt( "operator.deployment.xp.config.vhosts.nameTemplate", nodeGroup ) );
        meta.setLabels( new HashMap<>( info.xpDeploymentResource().getMetadata().getLabels() ) );
        meta.getLabels().put( cfgStr( "operator.helm.charts.Values.labels.managed" ), "true" );
        return meta;
    }
}
