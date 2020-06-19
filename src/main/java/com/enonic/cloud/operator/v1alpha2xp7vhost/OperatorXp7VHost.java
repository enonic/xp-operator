package com.enonic.cloud.operator.v1alpha2xp7vhost;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

@Singleton
public class OperatorXp7VHost
    extends InformerEventHandler<Xp7VHost>
{

    @Inject
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Override
    protected void init()
    {

    }

    @Override
    public void onNewAdd( final Xp7VHost newResource )
    {
        operatorXp7ConfigSync.handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final Xp7VHost oldResource, final Xp7VHost newResource )
    {
        if ( !oldResource.getXp7VHostSpec().equals( newResource.getXp7VHostSpec() ) )
        {
            operatorXp7ConfigSync.handle( newResource.getMetadata().getNamespace() );
        }
    }

    @Override
    public void onDelete( final Xp7VHost oldResource, final boolean b )
    {
        operatorXp7ConfigSync.handle( oldResource.getMetadata().getNamespace() );
    }
}
