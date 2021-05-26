package com.enonic.kubernetes.operator.v1alpha2xp7deployment;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * This operator class invalidates XP client cache if Xp7Deployment is deleted
 */
@ApplicationScoped
public class OperatorXpClientCacheInvalidate
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    XpClientCache xpClientCache;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.xp7DeploymentInformer() );
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
        // Invalidate XP client cache for deleted deployments
        xpClientCache.closeClients( oldResource.getMetadata().getNamespace() );
    }
}
