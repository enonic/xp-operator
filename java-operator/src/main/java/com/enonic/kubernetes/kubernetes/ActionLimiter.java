package com.enonic.kubernetes.kubernetes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.common.TaskRunner;

@NullMarked
public class ActionLimiter
{
    private static final Logger LOG = LoggerFactory.getLogger( ActionLimiter.class );

    private final String name;

    private final TaskRunner taskRunner;

    private final long delay;

    private final Map<String, KeyState> states = new ConcurrentHashMap<>();

    public ActionLimiter( final String name, final TaskRunner taskRunner, final long delay )
    {
        this.name = name;
        this.taskRunner = taskRunner;
        this.delay = delay;
    }

    public <T extends HasMetadata> void limit( final T r, final Consumer<T> c )
    {
        limit( r, x -> x.getMetadata().getNamespace() + "/" + x.getMetadata().getName(), () -> c.accept( r ) );
    }

    public <T> void limit( final T t, final Function<T, String> keyFunc, final Runnable r )
    {
        final String key = keyFunc.apply( t );
        states.compute( key, ( k, state ) -> {
            final KeyState s = state == null ? new KeyState() : state;
            if ( s.pending != null && s.pending.cancel( false ) )
            {
                LOG.debug( "Limiter '{}' canceled previously scheduled {}", name, k );
            }
            final long generation = ++s.generation;
            s.next = r;
            s.pending = taskRunner.scheduleOneTime( () -> fire( k, generation ), delay, TimeUnit.MILLISECONDS );
            LOG.debug( "Limiter '{}' will run {} after {} ms", name, k, delay );
            return s;
        } );
    }

    private void fire( final String key, final long generation )
    {
        Runnable action = start( key, generation );
        while ( action != null )
        {
            LOG.debug( "Limiter '{}' running {}", name, key );
            try
            {
                action.run();
            }
            catch ( Exception e )
            {
                LOG.error( "Limiter '{}' failed running {}", name, key, e );
            }
            action = finish( key );
        }
    }

    private @Nullable Runnable start( final String key, final long generation )
    {
        final Runnable[] action = {null};
        states.computeIfPresent( key, ( k, s ) -> {
            if ( s.generation != generation )
            {
                return s;
            }
            s.pending = null;
            if ( s.running )
            {
                LOG.debug( "Limiter '{}' runs {} again once the current run completes", name, k );
                s.rerun = true;
                return s;
            }
            s.running = true;
            action[0] = s.next;
            return s;
        } );
        return action[0];
    }

    private @Nullable Runnable finish( final String key )
    {
        final Runnable[] action = {null};
        states.computeIfPresent( key, ( k, s ) -> {
            if ( s.rerun )
            {
                s.rerun = false;
                action[0] = s.next;
                return s;
            }
            s.running = false;
            LOG.debug( "Limiter '{}' completed {}", name, k );
            return s.pending == null ? null : s;
        } );
        return action[0];
    }

    private static final class KeyState
    {
        @Nullable ScheduledFuture<?> pending;

        long generation;

        @Nullable Runnable next;

        boolean running;

        boolean rerun;
    }
}
