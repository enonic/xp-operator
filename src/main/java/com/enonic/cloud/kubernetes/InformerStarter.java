package com.enonic.cloud.kubernetes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class InformerStarter
{
    private static final Logger log = LoggerFactory.getLogger( InformerStarter.class );

    private static final Long startingDelay = 1 * 1000L; // TODO: Add to properties file

    @Inject
    SharedInformerFactory factory;

    void onStartup( @Observes StartupEvent _ev )
    {
        new Thread( () -> {
            try
            {
                Thread.sleep( startingDelay );
            }
            catch ( InterruptedException e )
            {
                // Ignore
            }
            log.info( "Starting informers" );
            factory.startAllRegisteredInformers();
        } ).start();
    }
}
