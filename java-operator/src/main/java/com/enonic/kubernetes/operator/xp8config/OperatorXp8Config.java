package com.enonic.kubernetes.operator.xp8config;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.onCondition;

/**
 * This operator class triggers ConfigMap sync on Xp8Config changes
 */
@Singleton
public class OperatorXp8Config
    extends InformerEventHandler<Xp8Config>
    implements ApplicationEventListener<ServerStartupEvent>
{
    @Inject
    OperatorConfigMapSync operatorConfigMapSync;

    @Inject
    Informers informers;

    @Override
    public void onApplicationEvent( ServerStartupEvent event )
    {
        listen( informers.xp8ConfigInformer() );
    }

    @Override
    public void onNewAdd( final Xp8Config newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Xp8Config oldResource, final Xp8Config newResource )
    {
        onCondition( newResource, this::handle, fieldEquals( oldResource, Xp8Config::getSpec ).negate() );
    }

    @Override
    public void onDelete( final Xp8Config oldResource, final boolean b )
    {
        handle( oldResource );
    }

    private void handle( final Xp8Config newResource )
    {
        // Sync config in namespace
        operatorConfigMapSync.handle( newResource.getMetadata().getNamespace() );
    }
}
