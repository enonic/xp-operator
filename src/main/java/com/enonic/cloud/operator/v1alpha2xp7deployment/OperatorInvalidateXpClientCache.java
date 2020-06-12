package com.enonic.cloud.operator.v1alpha2xp7deployment;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.InformerEventHandler;

public class OperatorInvalidateXpClientCache
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    SharedIndexInformer<Xp7Deployment> xp7DeploymentSharedIndexInformer;

    @Inject
    XpClientCache xpClientCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        listenToInformer( xp7DeploymentSharedIndexInformer );
    }

    @Override
    public void onAdd( final Xp7Deployment v1alpha2Xp7Deployment )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp7Deployment v1alpha2Xp7Deployment, final Xp7Deployment t1 )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final Xp7Deployment v1alpha2Xp7Deployment, final boolean b )
    {
        xpClientCache.invalidateCache( v1alpha2Xp7Deployment );
    }
}
