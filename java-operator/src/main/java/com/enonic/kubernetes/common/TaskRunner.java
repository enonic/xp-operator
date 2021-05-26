package com.enonic.kubernetes.common;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.enonic.kubernetes.common.Configuration.cfgInt;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;


@Singleton
public class TaskRunner
{
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( cfgInt( "operator.tasks.threads" ) );

    private static final Logger log = LoggerFactory.getLogger( TaskRunner.class );

    public TaskRunner()
    {
        singletonAssert(this, "constructor");
    }

    public void scheduleAtFixedRate( final Runnable command, final long initialDelay, final long period, final TimeUnit unit )
    {
        executor.scheduleAtFixedRate( () -> {
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
