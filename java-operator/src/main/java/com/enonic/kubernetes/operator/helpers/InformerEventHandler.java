package com.enonic.kubernetes.operator.helpers;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

import com.enonic.kubernetes.kubernetes.ResourceQuery;

import static com.enonic.kubernetes.common.Configuration.cfgLong;

public abstract class InformerEventHandler<R extends HasMetadata>
    implements ResourceEventHandler<R>
{
    private long maxAge;

    public void initialize()
    {
        long reSync = cfgLong( "operator.informers.reSync" );
        maxAge = ( reSync / 2L ) / 1000;
    }

    @Override
    public void onAdd( final R newResource )
    {
        ResourceQuery.resourceQuery( newResource ).
            youngerThen( maxAge ).
            get().
            ifPresent( this::onNewAdd );
    }

    protected abstract void onNewAdd( final R newResource );
    
}
