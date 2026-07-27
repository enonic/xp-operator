package com.enonic.kubernetes.operator.xp8deployment;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This operator class invalidates XP client cache if Xp8Deployment is deleted
 */
@Singleton
public class OperatorXpClientCacheInvalidate
    extends InformerEventHandler<Xp8Deployment>
    implements ApplicationEventListener<ServerStartupEvent>
{
    @Inject
    XpClientCache xpClientCache;

    @Inject
    Informers informers;

    @Override
    public void onApplicationEvent( ServerStartupEvent event )
    {
        listen( informers.xp8DeploymentInformer() );
    }

    @Override
    public void onNewAdd( final Xp8Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp8Deployment oldResource, final Xp8Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final Xp8Deployment oldResource, final boolean b )
    {
        // Invalidate XP client cache for deleted deployments
        xpClientCache.closeClients( oldResource.getMetadata().getNamespace() );
    }
}
