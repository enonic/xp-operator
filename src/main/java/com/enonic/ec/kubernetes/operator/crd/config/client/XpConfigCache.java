package com.enonic.ec.kubernetes.operator.crd.config.client;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;

@Singleton
public class XpConfigCache
    extends ResourceCache<XpConfigResource>
{
    private final XpConfigClient client;

    @Inject
    public XpConfigCache( XpConfigClientProducer XpConfigClientProducer )
    {
        super();
        this.client = XpConfigClientProducer.produce();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        // Set initial state of XP Configs
        initialize( client.client().inAnyNamespace().list().getItems() );

        client.client().inAnyNamespace().watch( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final XpConfigResource resource )
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
