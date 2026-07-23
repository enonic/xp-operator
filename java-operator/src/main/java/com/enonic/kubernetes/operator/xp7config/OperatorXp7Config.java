package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.crd.v1.Xp7Config;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.onCondition;

/**
 * This operator class triggers ConfigMap sync on Xp7Config changes
 */
@Singleton
public class OperatorXp7Config
    extends InformerEventHandler<Xp7Config>
    implements ApplicationEventListener<ServerStartupEvent>
{
    @Inject
    OperatorConfigMapSync operatorConfigMapSync;

    @Inject
    Informers informers;

    @Override
    public void onApplicationEvent( ServerStartupEvent event )
    {
        listen( informers.xp7ConfigInformer() );
    }

    @Override
    public void onNewAdd( final Xp7Config newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Xp7Config oldResource, final Xp7Config newResource )
    {
        onCondition( newResource, this::handle, fieldEquals( oldResource, Xp7Config::getSpec ).negate() );
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
