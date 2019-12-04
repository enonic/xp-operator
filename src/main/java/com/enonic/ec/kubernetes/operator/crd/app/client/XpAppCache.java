package com.enonic.ec.kubernetes.operator.crd.app.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;

@Singleton
public class XpAppCache
    extends ResourceCache<XpAppResource>
{
    private final XpAppClient client;

    @Inject
    public XpAppCache( XpAppClientProducer XpAppClientProducer )
    {
        super();
        this.client = XpAppClientProducer.produce();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        // Set initial state of XP Apps
        initialize( client.client().inAnyNamespace().list().getItems() );

        client.client().inAnyNamespace().watch( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final XpAppResource resource )
            {
                handleEvent( action, resource );
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
