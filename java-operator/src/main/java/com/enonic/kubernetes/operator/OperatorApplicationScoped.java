package com.enonic.kubernetes.operator;

import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class OperatorApplicationScoped
{
    private static final Logger log = LoggerFactory.getLogger( OperatorApplicationScoped.class );

    @ConfigProperty(name = "operator.tasks.initial.delay")
    Long initialDelay;

    @Inject
    TaskRunner taskRunner;

    public void schedule( long periodMs, Runnable runnable )
    {
        taskRunner.scheduleOneTime( () -> {
            long leftLimit = 1000L;
            long rightLimit = 20000L;
            long initialDelayMs = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
            log.info( String.format( "Adding schedule '%s' [delay: %d, period: %d]", runnable.getClass().getSimpleName(), initialDelayMs,
                periodMs ) );
            taskRunner.scheduleAtFixedRate( runnable, initialDelayMs, periodMs, TimeUnit.MILLISECONDS );
        }, initialDelay, TimeUnit.MILLISECONDS );
    }

    public <T> void listen( SharedIndexInformer<T> informer, ResourceEventHandler<T> handler )
    {
        taskRunner.scheduleOneTime( () -> {
            log.info( String.format( "Adding listener '%s'", handler.getClass().getSimpleName() ) );
            if (handler instanceof InformerEventHandler) {
                ((InformerEventHandler) handler).initialize();
            }
            informer.addEventHandler( handler );
        }, initialDelay, TimeUnit.MILLISECONDS );
    }
}
