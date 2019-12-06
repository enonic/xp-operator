package com.enonic.ec.kubernetes.operator.crd.deployment.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResourceList;

@Singleton
public class XpDeploymentCache
    extends ResourceCache<XpDeploymentResource, XpDeploymentResourceList>
{
    @Inject
    public XpDeploymentCache( XpDeploymentClientProducer xpDeploymentClientProducer )
    {
        super( getResourceFilter( xpDeploymentClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<XpDeploymentResource, XpDeploymentResourceList, Boolean, Watch, Watcher<XpDeploymentResource>> getResourceFilter(
        XpDeploymentClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final XpDeploymentResource resource )
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
