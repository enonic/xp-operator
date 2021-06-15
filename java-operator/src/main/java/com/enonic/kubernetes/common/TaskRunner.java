package com.enonic.kubernetes.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.enonic.kubernetes.common.Configuration.cfgInt;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;


@Singleton
public class TaskRunner
{
    private static final Logger log = LoggerFactory.getLogger( TaskRunner.class );

    private final ScheduledThreadPoolExecutor executor;

    public TaskRunner()
    {
        singletonAssert( this, "constructor" );
        executor = new ScheduledThreadPoolExecutor( cfgInt( "operator.tasks.threads" ), new ThreadFactoryBuilder().
            setNameFormat( "taskrunner-%d" ).
            build() );
    }

    public void scheduleAtFixedRate( final Runnable command, final long initialDelay, final long period, final TimeUnit unit )
    {
        executor.scheduleAtFixedRate( () -> {
            try {
                command.run();
            } catch (Exception e) {
                log.error( "Task " + command.getClass().getSimpleName() + " threw exception", e );
            }
        }, initialDelay, period, unit );
    }

    public void scheduleOneTime( final Runnable command, final long initialDelay, final TimeUnit unit )
    {
        executor.schedule( () -> {
            try {
                command.run();
            } catch (Exception e) {
                log.error( "Task " + command.getClass().getSimpleName() + " threw exception", e );
            }
        }, initialDelay, unit );
    }
}
