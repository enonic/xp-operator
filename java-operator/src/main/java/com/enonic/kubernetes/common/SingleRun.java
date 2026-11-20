package com.enonic.kubernetes.common;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class SingleRun
{
    private static final Logger LOG = LoggerFactory.getLogger( SingleRun.class );

    private static final String WHOLE = "";

    private final Set<String> running = ConcurrentHashMap.newKeySet();

    private final String name;

    public SingleRun( final String name )
    {
        this.name = name;
    }

    public void run( final Runnable body )
    {
        run( WHOLE, body );
    }

    public void run( final String key, final Runnable body )
    {
        if ( !running.add( key ) )
        {
            LOG.warn( "{} skipped{}: the previous run is still in progress", name, key.isEmpty() ? "" : " for " + key );
            return;
        }
        try
        {
            body.run();
        }
        finally
        {
            running.remove( key );
        }
    }
}
