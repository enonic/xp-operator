package com.enonic.ec.kubernetes.common.cache;

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

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;

@Singleton
public class IngressCache
    extends ResourceCache<Ingress>
{
    private final KubernetesClient client;

    @Inject
    public IngressCache( DefaultClientProducer defaultClientProducer )
    {
        super();
        this.client = defaultClientProducer.client();
    }

    private FilterWatchListMultiDeletable<Ingress, IngressList, Boolean, Watch, Watcher<Ingress>> getResourceFilter()
    {
        return client.extensions().ingresses().inAnyNamespace();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        // Set initial state of config maps
        initialize( getResourceFilter().list().getItems() );

        // Only watch XP config maps
        getResourceFilter().watch( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Ingress ingress )
            {
                handleEvent( action, ingress );
            }

            @Override
            public void onClose( final KubernetesClientException cause )
            {
                if ( cause != null )
                {
                    // This means the socket closed and we have a problem, best to
                    // let kubernetes just restart the operator pod.
                    cause.printStackTrace();
                    System.exit( -1 );
                }
            }
        } );
    }
}
