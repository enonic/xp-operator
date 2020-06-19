package com.enonic.cloud.operator.v1alpha2xp7config;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

@Singleton
public class OperatorXp7Config
    extends InformerEventHandler<Xp7Config>
{
    @Inject
    OperatorConfigMapSync operatorConfigMapSync;

    @Override
    protected void init()
    {

    }

    @Override
    public void onNewAdd( final Xp7Config newResource )
    {
        operatorConfigMapSync.handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final Xp7Config oldResource, final Xp7Config newResource )
    {
        if ( !oldResource.getXp7ConfigSpec().equals( newResource.getXp7ConfigSpec() ) )
        {
            operatorConfigMapSync.handle( newResource.getMetadata().getNamespace() );
        }
    }

    @Override
    public void onDelete( final Xp7Config oldResource, final boolean b )
    {
        operatorConfigMapSync.handle( oldResource.getMetadata().getNamespace() );
    }
}
