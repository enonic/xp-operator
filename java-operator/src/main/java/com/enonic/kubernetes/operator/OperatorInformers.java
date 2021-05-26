package com.enonic.kubernetes.operator;

import com.enonic.kubernetes.common.Exit;
import com.enonic.kubernetes.kubernetes.Informers;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.enonic.kubernetes.common.Configuration.cfgLong;

/**
 * This operator class kills the operator if it fails to keep informers in sync
 */
@ApplicationScoped
public class OperatorInformers
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorInformers.class );

    private final Map<Class<? extends HasMetadata>, Integer> failureMap = new HashMap<>();
    private final Map<Class<? extends HasMetadata>, String> resourceVersionMap = new HashMap<>();

    @Inject
    Operator operator;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        // Add hook that terminates the operator on informer errors
        informers.informerFactory().addSharedInformerEventListener( e -> {
            log.error( "Informer exception: " + e.getMessage(), e );
            Exit.exit( Exit.Code.INFORMER_FAILED, "Unrecoverable error with informers!" );
        } );

        // Schedule checks
        operator.schedule( cfgLong( "operator.informers.reSync" ), this );
    }

    @Override
    public void run()
    {
        // Check all informers
        for (Map.Entry<Class<? extends HasMetadata>, SharedIndexInformer> e : informers.allInformers().entrySet()) {
            checkInformer( e.getKey(), e.getValue() );
        }
    }

    private void checkInformer( Class<? extends HasMetadata> klass, SharedIndexInformer informer )
    {
        // Log when resource versions change
        String lastResourceVersion = resourceVersionMap.getOrDefault( klass, null );
        if (!Objects.equals( lastResourceVersion, informer.lastSyncResourceVersion() )) {
            log.debug( String.format( "Informer for %s resource version updated to %s", klass.getSimpleName(), informer.lastSyncResourceVersion() ) );
            resourceVersionMap.put( klass, informer.lastSyncResourceVersion() );
        }

        // Handle failures to sync
        Integer fails = failureMap.getOrDefault( klass, 0 );
        if (!informer.hasSynced()) {
            fails += 1;
            log.warn( String.format( "Informer for %s has failed sync %s times", klass.getSimpleName(), fails ) );

            if (fails == 3) {
                log.warn( String.format( "Informer for %s restart", klass.getSimpleName() ) );
                informer.stop();
                informer.run();
            } else if (fails >= 6) {
                Exit.exit( Exit.Code.INFORMER_FAILED, String.format( "Informer for %s cannot sync", klass.getSimpleName() ) );
            }
        } else {
            fails = 0;
        }
        failureMap.put( klass, fails );
    }
}
