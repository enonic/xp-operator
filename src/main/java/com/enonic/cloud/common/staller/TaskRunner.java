package com.enonic.cloud.common.staller;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class TaskRunner
{
    private static Logger log = LoggerFactory.getLogger( TaskRunner.class );

    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( 2 );

    private final static long defaultDelay = 2000L;

    private final static long defaultPeriod = 2000L;

    public ScheduledFuture<?> scheduleAtFixedRate( final Runnable command, final long initialDelay, final long period, final TimeUnit unit )
    {
        return executor.scheduleAtFixedRate( () -> {
            try
            {
                command.run();
            }
            catch ( Exception e )
            {
                log.error( "Task " + command.getClass().getSimpleName() + " threw exception", e );
            }
        }, initialDelay, period, unit );
    }

    public ScheduledFuture<?> scheduleAtFixedRate( final Runnable task )
    {
        return scheduleAtFixedRate( task, defaultDelay, defaultPeriod, TimeUnit.MILLISECONDS );
    }
}
