package com.enonic.kubernetes.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.kubernetes.common.TaskRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionLimiterTest
{
    private final List<Runnable> scheduled = new ArrayList<>();

    private ActionLimiter limiter;

    @BeforeEach
    void setUp()
    {
        final TaskRunner taskRunner = mock( TaskRunner.class );
        when( taskRunner.scheduleOneTime( any(), anyLong(), eq( TimeUnit.MILLISECONDS ) ) ).thenAnswer( invocation -> {
            scheduled.add( invocation.getArgument( 0 ) );
            return mock( ScheduledFuture.class );
        } );
        limiter = new ActionLimiter( "test", taskRunner, 1000L );
    }

    @Test
    void runsOnlyTheLatestOfABurst()
    {
        final List<String> runs = new ArrayList<>();

        limiter.limit( "ns", k -> k, () -> runs.add( "first" ) );
        limiter.limit( "ns", k -> k, () -> runs.add( "second" ) );
        scheduled.forEach( Runnable::run );

        assertThat( runs ).containsExactly( "second" );
    }

    @Test
    void runsOnceMoreAfterTheCurrentRunInsteadOfAlongsideIt()
        throws Exception
    {
        final CountDownLatch started = new CountDownLatch( 1 );
        final CountDownLatch release = new CountDownLatch( 1 );
        final AtomicInteger concurrent = new AtomicInteger();
        final AtomicInteger maxConcurrent = new AtomicInteger();
        final List<String> runs = new ArrayList<>();

        limiter.limit( "ns", k -> k, () -> track( concurrent, maxConcurrent, () -> {
            runs.add( "first" );
            started.countDown();
            await( release );
        } ) );
        final Thread first = Thread.ofVirtual().start( scheduled.get( 0 ) );
        started.await();

        limiter.limit( "ns", k -> k, () -> track( concurrent, maxConcurrent, () -> runs.add( "second" ) ) );
        scheduled.get( 1 ).run();
        assertThat( runs ).containsExactly( "first" );

        release.countDown();
        first.join();

        assertThat( runs ).containsExactly( "first", "second" );
        assertThat( maxConcurrent ).hasValue( 1 );
    }

    @Test
    void keysRunIndependently()
    {
        final List<String> runs = new ArrayList<>();

        limiter.limit( "a", k -> k, () -> runs.add( "a" ) );
        limiter.limit( "b", k -> k, () -> runs.add( "b" ) );
        scheduled.forEach( Runnable::run );

        assertThat( runs ).containsExactly( "a", "b" );
    }

    @Test
    void runsAgainAfterAFailedRun()
    {
        final List<String> runs = new ArrayList<>();

        limiter.limit( "ns", k -> k, () -> {
            throw new IllegalStateException( "failed" );
        } );
        scheduled.get( 0 ).run();

        limiter.limit( "ns", k -> k, () -> runs.add( "after" ) );
        scheduled.get( 1 ).run();

        assertThat( runs ).containsExactly( "after" );
    }

    private static void track( final AtomicInteger concurrent, final AtomicInteger maxConcurrent, final Runnable body )
    {
        maxConcurrent.accumulateAndGet( concurrent.incrementAndGet(), Math::max );
        try
        {
            body.run();
        }
        finally
        {
            concurrent.decrementAndGet();
        }
    }

    private static void await( final CountDownLatch latch )
    {
        try
        {
            latch.await();
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
    }
}
