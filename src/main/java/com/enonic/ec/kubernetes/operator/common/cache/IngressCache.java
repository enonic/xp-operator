package com.enonic.ec.kubernetes.operator.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.client.DefaultClientProducer;

@Singleton
public class IngressCache
    extends ResourceCache<Ingress, IngressList>
{
    @Inject
    public IngressCache( DefaultClientProducer defaultClientProducer )
    {
        super( getResourceFilter( defaultClientProducer.client() ) );
    }

    private static FilterWatchListMultiDeletable<Ingress, IngressList, Boolean, Watch, Watcher<Ingress>> getResourceFilter(
        KubernetesClient client )
    {
        return client.extensions().ingresses().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Ingress resource )
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
