package com.enonic.cloud.common.staller;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.enonic.cloud.common.Configuration.cfgInt;


@Singleton
public class TaskRunner
{
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( cfgInt( "operator.tasks.threads" ) );

    private static Logger log = LoggerFactory.getLogger( TaskRunner.class );

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
}
