package com.enonic.cloud.operator.operators.v1alpha1.xp7app;

import java.util.HashMap;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.operators.common.OperatorNamespaced;
import com.enonic.cloud.operator.operators.common.ResourceInfoXp7DeploymentDependant;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.common.queues.OperatorChangeQueues;
import com.enonic.cloud.operator.operators.v1alpha1.xp7app.info.DiffXp7App;
import com.enonic.cloud.operator.operators.v1alpha1.xp7app.info.ImmutableInfoXp7App;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.common.Configuration.cfgStrFmt;


@ApplicationScoped
public class OperatorXp7App
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7App.class );

    @Inject
    Clients clients;

    @Inject
    OperatorChangeQueues changeQueues;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getAppCache().addEventListener( this::watchApps );
        log.info( "Started listening for Xp7App events" );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchApps( final String actionId, final Watcher.Action action, final Optional<V1alpha1Xp7App> oldResource,
                            final Optional<V1alpha1Xp7App> newResource )
    {
        // Create info about the CRD
        Optional<ResourceInfoXp7DeploymentDependant<V1alpha1Xp7App, DiffXp7App>> i = getInfo( action, () -> ImmutableInfoXp7App.builder().
            caches( caches ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> runCommands( actionId, commandBuilder -> {
            if ( isNamespaceBeingTerminated( info ) )
            {
                // Everything is about to be deleted, just ignore
                return;
            }

            if ( info.resourceBeingRestoredFromBackup() )
            {
                // This is a backup restore, just ignore
                return;
            }

            String nodeGroup = cfgStr( "operator.helm.charts.Values.allNodesKey" );
            changeQueues.getV1alpha2Xp7ConfigResourceChangeQueue().
                enqueue( actionId, ImmutableXp7AppAggregator.builder().
                    caches( caches ).
                    clients( clients ).
                    metadata( createMetadata( info, nodeGroup ) ).
                    nodeGroup( nodeGroup ).
                    build() );
        } ) );
    }

    private ObjectMeta createMetadata( final ResourceInfoXp7DeploymentDependant<V1alpha1Xp7App, DiffXp7App> info, final String nodeGroup )
    {
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace( info.namespace() );
        meta.setName( cfgStrFmt( "operator.deployment.xp.config.deploy.nameTemplate", nodeGroup ) );
        meta.setLabels( new HashMap<>( info.xpDeploymentResource().getMetadata().getLabels() ) );
        meta.getLabels().put( cfgStr( "operator.helm.charts.Values.labelKeys.managed" ), "true" );
        return meta;
    }
}