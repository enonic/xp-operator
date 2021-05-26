package com.enonic.kubernetes.operator.helpers;

import com.enonic.kubernetes.kubernetes.Predicates;
import com.enonic.kubernetes.operator.Operator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import javax.inject.Inject;

import static com.enonic.kubernetes.common.Configuration.cfgLong;

public abstract class InformerEventHandler<R extends HasMetadata>
    implements ResourceEventHandler<R>
{
    @Inject
    Operator operator;

    private long maxAge;

    protected void listen( SharedIndexInformer<R> informer )
    {
        // Set max age of resources to trigger onNewAdd event
        long reSync = cfgLong( "operator.informers.reSync" );
        maxAge = (reSync / 2L) / 1000;

        operator.listen( informer, this );
    }

    protected void scheduleSync( Runnable runnable )
    {
        operator.schedule( cfgLong( "operator.tasks.sync.interval" ), runnable );
    }

    public void initialize()
    {
        // TODO: REMOVE
    }

    @Override
    public void onAdd( final R newResource )
    {
        if (Predicates.youngerThan( maxAge ).test( newResource )) {
            onNewAdd( newResource );
        }
    }

    protected abstract void onNewAdd( final R newResource );

}
