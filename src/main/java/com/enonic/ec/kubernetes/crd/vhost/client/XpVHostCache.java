package com.enonic.ec.kubernetes.crd.vhost.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;

@Singleton
public class XpVHostCache
    extends ResourceCache<XpVHostResource>
{
    private final XpVHostClient client;

    @Inject
    public XpVHostCache( XpVHostClientProducer XpVHostClientProducer )
    {
        super();
        this.client = XpVHostClientProducer.produce();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        initialize( client.client().inAnyNamespace().list().getItems() );
        client.client().inAnyNamespace().watch( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final XpVHostResource deployment )
            {
                handleEvent( action, deployment );
            }

            @Override
            public void onClose( final KubernetesClientException cause )
            {
                // This means the socket closed and we have a problem, best to let kubernetes
                // just restart the controller pod.
                cause.printStackTrace();
                System.exit( -1 );
            }
        } );
    }
}
