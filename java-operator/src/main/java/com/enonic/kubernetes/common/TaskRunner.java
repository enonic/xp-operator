package com.enonic.kubernetes.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.enonic.kubernetes.common.Configuration.cfgInt;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;


@Singleton
public class TaskRunner
{
    private static final Logger LOG = LoggerFactory.getLogger( TaskRunner.class );

    private final ScheduledExecutorService scheduledExecutorService;

    private final ExecutorService executor;

    public TaskRunner()
    {
        singletonAssert( this, "constructor" );
        scheduledExecutorService =
            Executors.newScheduledThreadPool( 4, threadFactory( "task-scheduler-" ) );

        executor = new ThreadPoolExecutor( 1, cfgInt( "operator.tasks.threads" ), 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                                           threadFactory( "task-runner-" ),
                                           new ThreadPoolExecutor.CallerRunsPolicy() );
    }

    public void scheduleAtFixedRate( final Runnable command, final long initialDelay, final long period, final TimeUnit unit )
    {
        scheduledExecutorService.scheduleAtFixedRate( () -> schedulable( command ), initialDelay, period, unit );
    }

    public ScheduledFuture<?> scheduleOneTime( final Runnable command, final long initialDelay, final TimeUnit unit )
    {
        return scheduledExecutorService.schedule( () -> schedulable( command ), initialDelay, unit );
    }

    private void schedulable( final Runnable command )
    {
        try
        {
            executor.execute( () -> {
                try
                {
                    command.run();
                }
                catch ( Throwable e )
                {
                    LOG.error( "Task {} threw exception", command.getClass().getSimpleName(), e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.error( "Task {} cannot be executed", command.getClass().getSimpleName(), e );
        }
    }

    private static ThreadFactory threadFactory( final String prefix )
    {
        final ThreadFactory factory = Executors.defaultThreadFactory();
        final AtomicLong count = new AtomicLong();
        return runnable -> {
            final Thread thread = factory.newThread( runnable );
            thread.setName( prefix + count.getAndIncrement() );
            return thread;
        };
    }
}
