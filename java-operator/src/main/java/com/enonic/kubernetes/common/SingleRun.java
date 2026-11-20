package com.enonic.kubernetes.common;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class SingleRun
{
    private static final Logger LOG = LoggerFactory.getLogger( SingleRun.class );

    private final AtomicBoolean running = new AtomicBoolean();

    private final String name;

    public SingleRun( final String name )
    {
        this.name = name;
    }

    public void run( final Runnable body )
    {
        if ( !running.compareAndSet( false, true ) )
        {
            LOG.warn( "{} skipped: the previous run is still in progress", name );
            return;
        }
        try
        {
            body.run();
        }
        finally
        {
            running.set( false );
        }
    }
}
