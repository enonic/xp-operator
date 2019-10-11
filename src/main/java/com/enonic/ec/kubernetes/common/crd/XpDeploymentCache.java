package com.enonic.ec.kubernetes.common.crd;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.common.crd.XpDeployment.XpDeploymentResource;

@Singleton
public class XpDeploymentCache
    extends ResourceCache<XpDeploymentResource>
{

    private CrdClientsProducer.XpDeploymentClient client;

    @Inject
    public XpDeploymentCache( final CrdClientsProducer.XpDeploymentClient client )
    {
        super();
        this.client = client;
    }

    void onStartup( @Observes StartupEvent _ev )
    {
        initialize( client.getClient().inAnyNamespace().list().getItems() );
        client.getClient().inAnyNamespace().watch( new Watcher<XpDeploymentResource>()
        {
            @Override
            public void eventReceived( final Action action, final XpDeploymentResource deployment )
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
