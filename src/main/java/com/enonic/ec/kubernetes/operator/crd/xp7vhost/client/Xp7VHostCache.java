package com.enonic.ec.kubernetes.operator.crd.xp7vhost.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResourceList;

@Singleton
public class Xp7VHostCache
    extends ResourceCache<Xp7VHostResource, Xp7VHostResourceList>
{
    @Inject
    public Xp7VHostCache( Xp7VHostClientProducer xp7VHostClientProducer )
    {
        super( getResourceFilter( xp7VHostClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<Xp7VHostResource, Xp7VHostResourceList, Boolean, Watch, Watcher<Xp7VHostResource>> getResourceFilter(
        Xp7VHostClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<Xp7VHostResource>()
        {
            @Override
            public void eventReceived( final Action action, final Xp7VHostResource resource )
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
