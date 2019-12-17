package com.enonic.ec.kubernetes.operator.crd.xp7app.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResourceList;

@Singleton
public class Xp7AppCache
    extends ResourceCache<Xp7AppResource, Xp7AppResourceList>
{
    @Inject
    public Xp7AppCache( Xp7AppClientProducer xp7AppClientProducer )
    {
        super( getResourceFilter( xp7AppClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<Xp7AppResource, Xp7AppResourceList, Boolean, Watch, Watcher<Xp7AppResource>> getResourceFilter(
        Xp7AppClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Xp7AppResource resource )
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
