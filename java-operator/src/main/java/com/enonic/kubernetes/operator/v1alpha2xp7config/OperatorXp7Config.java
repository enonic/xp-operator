package com.enonic.kubernetes.operator.v1alpha2xp7config;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;

import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.onCondition;

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
