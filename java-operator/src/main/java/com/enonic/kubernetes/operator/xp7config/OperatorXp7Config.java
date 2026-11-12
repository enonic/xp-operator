package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.onCondition;

/**
 * This operator class triggers ConfigMap sync on Xp7Config changes
 */
@ApplicationScoped
public class OperatorXp7Config
    extends InformerEventHandler<Xp7Config>
{
    @Inject
    OperatorConfigMapSync operatorConfigMapSync;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
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
