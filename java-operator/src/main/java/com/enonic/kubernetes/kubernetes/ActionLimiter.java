package com.enonic.kubernetes.kubernetes;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.common.TaskRunner;

public class ActionLimiter
{
    private static final Logger log = LoggerFactory.getLogger( ActionLimiter.class );

    private final String name;

    private final TaskRunner taskRunner;

    private final Map<String, ScheduledFuture<?>> ops = new ConcurrentHashMap<>();

    private final Map<String, Instant> cooldown = new ConcurrentHashMap<>();

    private final long delay;

    private final long cooldownPeriod;

    private final AtomicLong cleanupLastRun = new AtomicLong( 0 );

    public ActionLimiter( final String name, final TaskRunner taskRunner, final long delay, final long cooldownPeriod )
    {
        this.name = name;
        this.taskRunner = taskRunner;
        this.delay = delay;
        this.cooldownPeriod = cooldownPeriod;
    }

    public ActionLimiter( final String name, final TaskRunner taskRunner, final long delay )
    {
        this( name, taskRunner, delay, 0L );
    }

    public <T extends HasMetadata> void limit( T r, Consumer<T> c )
    {
        limit( r, x -> x.getMetadata().getNamespace() + "/" + x.getMetadata().getName(), () -> c.accept( r ) );
    }

    public <T> void limit( T t, Function<T, String>keyFunc, Runnable r )
    {
        final String key = keyFunc.apply( t );
        cleanup();
        final boolean toCooldown = cooldown( key );
        if ( toCooldown )
        {
            log.debug( "Limiter '{}' will not process further tasks {} for {} ms", name, key, cooldownPeriod );
            return;
        }
        schedule( key, r );
    }

    private void cleanup()
    {
        long nowMs = System.currentTimeMillis();
        long last = cleanupLastRun.get();

        if ( nowMs - last >= 1000 && cleanupLastRun.compareAndSet( last, nowMs ) )
        {
            log.trace( "Limiter '{}' cleanup", name );

            Instant now = Instant.ofEpochMilli( nowMs );
            cooldown.entrySet().removeIf( e -> e.getValue().isBefore( now ) );
            ops.entrySet().removeIf( e -> e.getValue().isDone() );
        }
    }

    private boolean cooldown( final String key )
    {
        final Instant now = Instant.now();
        if ( cooldownPeriod > 0L )
        {
            final boolean[] isPresent = {false};
            cooldown.compute( key, ( k, v ) -> {
                if ( v == null || now.isAfter( v ) )
                {
                    return now.plusMillis( cooldownPeriod );
                }
                else
                {
                    isPresent[0] = true;
                    return v;
                }
            } );

            if ( isPresent[0] )
            {
                log.debug( "Limiter '{}' skips tasks {} for now", name, key );
                return true;
            }
        }
        return false;
    }

    private void schedule( final String key, final Runnable r )
    {
        ops.compute( key, ( k, v ) -> {
            if ( v != null )
            {
                final boolean canceled = v.cancel( false );
                if ( canceled )
                {
                    log.debug( "Limiter '{}' canceled previously scheduled {}", name, k );
                }
                else
                {
                    log.trace( "Limiter '{}' could not cancel previously scheduled {}. It was likely already completed", name, k );
                }
            }
            final ScheduledFuture<?> scheduledFuture = taskRunner.scheduleOneTime( () -> {
                log.debug( "Limiter '{}' running {} after {} ms", name, k, delay );
                r.run();
                log.debug( "Limiter '{}' completed {}", name, k );
            }, delay, TimeUnit.MILLISECONDS );

            log.debug( "Limiter '{}' will run {} after {} ms", name, k, delay );
            return scheduledFuture;
        } );
    }
}
