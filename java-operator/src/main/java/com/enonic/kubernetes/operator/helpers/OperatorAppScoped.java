package com.enonic.kubernetes.operator.helpers;

import com.enonic.kubernetes.common.TaskRunner;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public abstract class OperatorAppScoped
{
    private static final Logger log = LoggerFactory.getLogger( OperatorAppScoped.class );

    private static final Timer timer = new Timer();

    @Inject
    TaskRunner taskRunner;

    void onStart( @Observes StartupEvent ev )
    {
        onStartup();
    }

    void onStop( @Observes ShutdownEvent ev )
    {
        onShutdown();
    }

    protected final void schedule( Runnable runnable, long periodMs )
    {
        timer.schedule( new TimerTask()
        {
            @Override
            public void run()
            {
                long leftLimit = 1000L;
                long rightLimit = 10000L;
                long initialDelayMs = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
                log.info( String.format( "Adding schedule '%s' [delay: %d, period: %d]", runnable.getClass().getSimpleName(), initialDelayMs,
                    periodMs ) );
                taskRunner.scheduleAtFixedRate( runnable, initialDelayMs, periodMs, TimeUnit.MILLISECONDS );
            }
        }, 10000L );
    }

    protected void onStartup()
    {

    }

    protected void onShutdown()
    {

    }
}
