package com.enonic.kubernetes.operator.helpers;

import com.enonic.kubernetes.kubernetes.Predicates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

import static com.enonic.kubernetes.common.Configuration.cfgLong;

public abstract class InformerEventHandler<R extends HasMetadata>
    implements ResourceEventHandler<R>
{
    private long maxAge;

    public void initialize()
    {
        long reSync = cfgLong( "operator.informers.reSync" );
        maxAge = (reSync / 2L) / 1000;
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
