package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResourceList;

@Singleton
public class Xp7DeploymentCache
    extends ResourceCache<Xp7DeploymentResource, Xp7DeploymentResourceList>
{

    protected Xp7DeploymentCache()
    {
        // Only for testing
        super();
    }

    @Inject
    public Xp7DeploymentCache( Xp7DeploymentClientProducer xp7DeploymentClientProducer )
    {
        super( getResourceFilter( xp7DeploymentClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<Xp7DeploymentResource, Xp7DeploymentResourceList, Boolean, Watch, Watcher<Xp7DeploymentResource>> getResourceFilter(
        Xp7DeploymentClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Xp7DeploymentResource resource )
            {
                watcherHandleEvent( action, resource );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
