package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.kubernetes.ActionLimiter;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.commands.ImmutableK8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandAction;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Instant;

import static com.enonic.kubernetes.kubernetes.Predicates.*;
import static com.enonic.kubernetes.operator.helpers.PasswordGenerator.getRandomScramble;

/**
 * This operator class sends events on ConfigMap changes
 */
@ApplicationScoped
public class OperatorConfigMapEvent
    extends InformerEventHandler<ConfigMap>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorConfigMapEvent.class );

    @Inject
    Clients clients;

    @Inject
    Informers informers;

    @Inject
    TaskRunner taskRunner;

    ActionLimiter limiter;

    void onStart( @Observes StartupEvent ev )
    {
        limiter = new ActionLimiter( this.getClass().getSimpleName(), taskRunner, 1000L );
        listen( informers.configMapInformer() );
    }

    @Override
    protected void onNewAdd( final ConfigMap newCm )
    {
        onCondition( newCm, cm -> {
            log.debug("onNew ConfigMap: {} in {}", cm.getMetadata().getNamespace(), cm.getMetadata().getName());
            this.handle(cm);
        }, isEnonicManaged() );
    }

    @Override
    public void onUpdate( final ConfigMap oldCm, final ConfigMap newCm )
    {
        if (dataNotEquals( oldCm ).and( isEnonicManaged() ).test( newCm )) {
            log.debug("onUpdate ConfigMap: {} in {}", newCm.getMetadata().getNamespace(), newCm.getMetadata().getName());
            limiter.limit( newCm, this::handle );
        }
    }

    @Override
    public void onDelete( final ConfigMap oldCm, final boolean b )
    {
        // Do nothing
    }

    private void handle( final ConfigMap newCm )
    {
        String eventName = String.format(
            "%s.%s",
            newCm.getMetadata().getName(),
            getRandomScramble( "0123456789abcdefghijklmnopqrstuvwxyz", 16 )
        );

        String eventTimestamp = Instant.now().toString();
        Event ev = new EventBuilder()
            //Metadata
            .withNewMetadata()
            .withNamespace( newCm.getMetadata().getNamespace() )
            .withName( eventName )
            .endMetadata()
            // Involved
            .withNewInvolvedObject()
            .withApiVersion( newCm.getApiVersion() )
            .withFieldPath( "data" )
            .withKind( newCm.getKind() )
            .withName( newCm.getMetadata().getName() )
            .withNamespace( newCm.getMetadata().getNamespace() )
            .withResourceVersion( newCm.getMetadata().getResourceVersion() )
            .withUid( newCm.getMetadata().getUid() )
            .endInvolvedObject()
            // Souce
            .withNewSource()
            .withComponent( "xp-operator" )
            .endSource()
            // Other
            .withFirstTimestamp( eventTimestamp )
            .withLastTimestamp( eventTimestamp )
            .withReason( "ConfigModified" )
            .withMessage( "ConfigMap modified" )
            .withType( "Normal" )
            .build();

        // Send event
        ImmutableK8sCommand.builder().
            action( K8sCommandAction.CREATE ).
            resource( ev ).
            wrappedRunnable( () -> clients.k8s().v1().events().createOrReplace( ev ) ).
            build().
            run();
    }
}
