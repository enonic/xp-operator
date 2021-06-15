package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.common.TaskRunner;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActionLimiter
{
    private static final Logger log = LoggerFactory.getLogger( ActionLimiter.class );

    private final TaskRunner taskRunner;

    private final Map<Integer, Runnable> ops;

    public ActionLimiter( final TaskRunner taskRunner )
    {
        this.taskRunner = taskRunner;
        this.ops = new ConcurrentHashMap<>();
    }

    public <T> void limit( long delay, T t, Function<T, Object> hashFunc, Runnable r )
    {
        int key = hashFunc.apply( t ).hashCode();
        ops.put( key, r );
        taskRunner.scheduleOneTime( () -> maybeRun( key ), delay, TimeUnit.MILLISECONDS );
        log.debug( String.format( "Limiter with delay %d scheduled for: %s", delay, r) );
    }

    public <T> void limit( long delay, T t, Function<T, Object> hashFunc, Consumer<T> c )
    {
        limit( delay, t, hashFunc, () -> c.accept( t ) );
    }

    public <T extends HasMetadata> void limit( long delay, T r, Consumer<T> c )
    {
        limit( delay, r, x -> String.format( "%s%s", x.getMetadata().getNamespace(), x.getMetadata().getName() ), c );
    }

    private void maybeRun( int m )
    {
        Runnable r = ops.remove( m );
        if (r != null) {
            log.debug( String.format( "Limiter running for for: %s", r) );
            r.run();
        }
    }
}
