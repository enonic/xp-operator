package com.enonic.ec.kubernetes.operator.crd.config.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResourceList;

@Singleton
public class XpConfigCache
    extends ResourceCache<XpConfigResource, XpConfigResourceList>
{
    @Inject
    public XpConfigCache( XpConfigClientProducer xpConfigClientProducer )
    {
        super( getResourceFilter( xpConfigClientProducer.produce() ) );
    }

    private static FilterWatchListDeletable<XpConfigResource, XpConfigResourceList, Boolean, Watch, Watcher<XpConfigResource>> getResourceFilter(
        XpConfigClient client )
    {
        return client.client().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final XpConfigResource resource )
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
