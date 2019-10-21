package com.enonic.ec.kubernetes.deployment;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ResourceCache;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;

@Singleton
public class XpDeploymentCache
    extends ResourceCache<XpDeploymentResource>
{

    private final XpDeploymentClient client;

    @Inject
    public XpDeploymentCache( XpDeploymentClientProducer xpDeploymentClientProducer )
    {
        super();
        this.client = xpDeploymentClientProducer.produce();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        initialize( client.client().inAnyNamespace().list().getItems() );
        client.client().inAnyNamespace().watch( new Watcher<>()
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
