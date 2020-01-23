package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResourceList;

@Singleton
public class Xp7ConfigCache
    extends ResourceCache<Xp7ConfigResource, Xp7ConfigResourceList>
{
    @Inject
    public Xp7ConfigCache( Xp7ConfigClientProducer xp7ConfigClientProducer )
    {
        super( getResourceFilter( xp7ConfigClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<Xp7ConfigResource, Xp7ConfigResourceList, Boolean, Watch, Watcher<Xp7ConfigResource>> getResourceFilter(
        Xp7ConfigClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Xp7ConfigResource resource )
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
