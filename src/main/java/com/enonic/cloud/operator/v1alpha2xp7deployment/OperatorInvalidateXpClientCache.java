package com.enonic.cloud.operator.v1alpha2xp7deployment;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7DeploymentCache;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

public class OperatorInvalidateXpClientCache
    implements ResourceEventHandler<V1alpha2Xp7Deployment>
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7DeploymentCache v1alpha2Xp7DeploymentCache;

    @Inject
    XpClientCache xpClientCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        v1alpha2Xp7DeploymentCache.addEventListener( this );
    }

    @Override
    public void onAdd( final V1alpha2Xp7Deployment v1alpha2Xp7Deployment )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final V1alpha2Xp7Deployment v1alpha2Xp7Deployment, final V1alpha2Xp7Deployment t1 )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final V1alpha2Xp7Deployment v1alpha2Xp7Deployment, final boolean b )
    {
        xpClientCache.invalidateCache( v1alpha2Xp7Deployment );
    }
}
