package com.enonic.kubernetes.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleRunTest
{
    @Test
    void skipsWhileThePreviousRunIsInProgress()
        throws Exception
    {
        final SingleRun singleRun = new SingleRun( "test" );
        final CountDownLatch started = new CountDownLatch( 1 );
        final CountDownLatch release = new CountDownLatch( 1 );
        final AtomicInteger runs = new AtomicInteger();

        final Thread first = Thread.ofVirtual().start( () -> singleRun.run( () -> {
            runs.incrementAndGet();
            started.countDown();
            await( release );
        } ) );
        started.await();

        singleRun.run( runs::incrementAndGet );
        assertThat( runs ).hasValue( 1 );

        release.countDown();
        first.join();

        singleRun.run( runs::incrementAndGet );
        assertThat( runs ).hasValue( 2 );
    }

    @Test
    void runsAgainAfterAFailedRun()
    {
        final SingleRun singleRun = new SingleRun( "test" );
        final AtomicInteger runs = new AtomicInteger();

        assertThatThrownBy( () -> singleRun.run( () -> {
            throw new IllegalStateException( "failed" );
        } ) ).isInstanceOf( IllegalStateException.class );

        singleRun.run( runs::incrementAndGet );
        assertThat( runs ).hasValue( 1 );
    }

    @Test
    void keysRunIndependently()
        throws Exception
    {
        final SingleRun singleRun = new SingleRun( "test" );
        final CountDownLatch started = new CountDownLatch( 1 );
        final CountDownLatch release = new CountDownLatch( 1 );
        final AtomicInteger runs = new AtomicInteger();

        final Thread first = Thread.ofVirtual().start( () -> singleRun.run( "a", () -> {
            started.countDown();
            await( release );
        } ) );
        started.await();

        singleRun.run( "a", runs::incrementAndGet );
        singleRun.run( "b", runs::incrementAndGet );
        assertThat( runs ).hasValue( 1 );

        release.countDown();
        first.join();
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
