package com.enonic.cloud.operator.v1alpha2xp7deployment;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

@Singleton
public class OperatorXpClientCacheInvalidate
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    XpClientCache xpClientCache;

    @Override
    protected void init()
    {
        // Do nothing
    }

    @Override
    public void onNewAdd( final Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final Xp7Deployment oldResource, final boolean b )
    {
        xpClientCache.invalidateCache( oldResource.getMetadata().getNamespace() );
    }
}
