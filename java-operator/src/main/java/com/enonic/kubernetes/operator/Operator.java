package com.enonic.kubernetes.operator;

import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

/**
 * This operator class starts informers and has handy methods to listen to informers and schedule tasks
 */
@Singleton
public class Operator
{
    private static final Logger log = LoggerFactory.getLogger( Operator.class );

    @ConfigProperty(name = "operator.tasks.initial.delay")
    Long initialDelay;

    @Inject
    TaskRunner taskRunner;

    @Inject
    Informers informers;

    public Operator()
    {
        singletonAssert( this, "constructor" );
    }

    void onStart( @Observes StartupEvent ev )
    {
        taskRunner.scheduleOneTime( () -> {
            log.info( "Starting informers" );
            informers.informerFactory().startAllRegisteredInformers();
        }, initialDelay + 5000L, TimeUnit.MILLISECONDS );
    }

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
