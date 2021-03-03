package com.enonic.kubernetes.operator.v1alpha2xp7deployment;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;

/**
 * This operator class invalidates XP client cache if Xp7Deployment is deleted
 */
@Singleton
public class OperatorXpClientCacheInvalidate
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    XpClientCache xpClientCache;

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
        // Invalidate XP client cache for deleted deployments
        xpClientCache.invalidateCache( oldResource.getMetadata().getNamespace() );
    }
}
