package com.enonic.ec.kubernetes.operator.crd.vhost.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResourceList;

@Singleton
public class XpVHostCache
    extends ResourceCache<XpVHostResource, XpVHostResourceList>
{
    @Inject
    public XpVHostCache( XpVHostClientProducer xpVHostClientProducer )
    {
        super( getResourceFilter( xpVHostClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<XpVHostResource, XpVHostResourceList, Boolean, Watch, Watcher<XpVHostResource>> getResourceFilter(
        XpVHostClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<XpVHostResource>()
        {
            @Override
            public void eventReceived( final Action action, final XpVHostResource resource )
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
