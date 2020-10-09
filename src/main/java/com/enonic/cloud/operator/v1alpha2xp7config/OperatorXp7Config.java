package com.enonic.cloud.operator.v1alpha2xp7config;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

/**
 * This operator class triggers ConfigMap sync on Xp7Config changes
 */
@Singleton
public class OperatorXp7Config
    extends InformerEventHandler<Xp7Config>
{
    @Inject
    OperatorConfigMapSync operatorConfigMapSync;

    @Override
    public void onNewAdd( final Xp7Config newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Xp7Config oldResource, final Xp7Config newResource )
    {
        if ( !oldResource.getXp7ConfigSpec().equals( newResource.getXp7ConfigSpec() ) )
        {
            // If the spec changed, roll out the update
            handle( newResource );
        }
    }

    @Override
    public void onDelete( final Xp7Config oldResource, final boolean b )
    {
        handle( oldResource );
    }

    private void handle( final Xp7Config newResource )
    {
        // Sync config in namespace
        operatorConfigMapSync.handle( newResource.getMetadata().getNamespace() );
    }
}
