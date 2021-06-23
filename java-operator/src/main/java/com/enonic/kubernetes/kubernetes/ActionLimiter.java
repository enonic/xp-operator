package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.common.TaskRunner;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.vertx.core.impl.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActionLimiter
{
    private static final Logger log = LoggerFactory.getLogger( ActionLimiter.class );

    private final String name;

    private final TaskRunner taskRunner;

    private final Map<String, Runnable> ops;

    private final Set<String> blocks;

    private final long delay;
    private final long block;

    public ActionLimiter( final String name, final TaskRunner taskRunner, final long delay, final long block )
    {
        this.name = name;
        this.taskRunner = taskRunner;
        this.ops = new ConcurrentHashMap<>();
        this.blocks = new ConcurrentHashSet<>();
        this.delay = delay;
        this.block = block;
    }

    public ActionLimiter( final String name, final TaskRunner taskRunner, final long delay )
    {
        this( name, taskRunner, delay, 0L );
    }

    public <T> void limit( T t, Function<T, String> hashFunc, Runnable r )
    {
        final String key = hashFunc.apply( t );

        // This limiter has blocks
        if (block > 0L) {
            // If this key is currently blocked, return
            if (blocks.contains( key )) {
                return;
            }

            // Add block and schedule to remove the block
            blocks.add( key );
            taskRunner.scheduleOneTime( () -> {
                blocks.remove( key );
                log.debug( String.format( "Limiter '%s' unblocked: %s", name, key ) );
            }, block, TimeUnit.MILLISECONDS );
            log.debug( String.format( "Limiter '%s' blocked for %d: %s", name, block, key ) );
        }

        // Schedule run
        ops.put( key, r );
        taskRunner.scheduleOneTime( () -> maybeRun( key ), delay, TimeUnit.MILLISECONDS );
        log.debug( String.format( "Limiter '%s' with delay %d scheduled for: %s", name, delay, key ) );
    }

    public <T> void limit( T t, Function<T, String> hashFunc, Consumer<T> c )
    {
        limit( t, hashFunc, () -> c.accept( t ) );
    }

    public <T extends HasMetadata> void limit( T r, Consumer<T> c )
    {
        limit( r, x -> String.format( "%s/%s", x.getMetadata().getNamespace(), x.getMetadata().getName() ), c );
    }

    private void maybeRun( String key )
    {
        Runnable r = ops.remove( key );
        if (r != null) {
            log.debug( String.format( "Limiter '%s' running after %d delay: %s", name, delay, r ) );
            r.run();
        }
    }
}
