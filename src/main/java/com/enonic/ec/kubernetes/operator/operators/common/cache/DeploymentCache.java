package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha2Xp7DeploymentList;

@Singleton
public class DeploymentCache
    extends Cache<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList>
{
    private final Clients clients;

    @Inject
    public DeploymentCache( Clients clients )
    {
        super( 1 );
        this.clients = clients;
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        FilterWatchListMultiDeletable<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList, Boolean, Watch, Watcher<V1alpha2Xp7Deployment>>
            filter = clients.
            getDeploymentClient().
            inAnyNamespace();
        startWatcher( filter, new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final V1alpha2Xp7Deployment v1alpha2Xp7Deployment )
            {
                watcherEventReceived( action, v1alpha2Xp7Deployment );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
