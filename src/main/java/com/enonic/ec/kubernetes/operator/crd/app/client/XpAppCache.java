package com.enonic.ec.kubernetes.operator.crd.app.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResourceList;

@Singleton
public class XpAppCache
    extends ResourceCache<XpAppResource, XpAppResourceList>
{
    @Inject
    public XpAppCache( XpAppClientProducer xpAppClientProducer )
    {
        super( getResourceFilter( xpAppClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<XpAppResource, XpAppResourceList, Boolean, Watch, Watcher<XpAppResource>> getResourceFilter(
        XpAppClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final XpAppResource resource )
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
