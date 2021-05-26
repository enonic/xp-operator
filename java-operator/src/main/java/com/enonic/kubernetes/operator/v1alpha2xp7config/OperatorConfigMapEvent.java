package com.enonic.kubernetes.operator.v1alpha2xp7config;

import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.commands.ImmutableK8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandAction;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.events.v1beta1.Event;
import io.fabric8.kubernetes.api.model.events.v1beta1.EventBuilder;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Instant;

import static com.enonic.kubernetes.kubernetes.Predicates.dataEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.isEnonicManaged;
import static com.enonic.kubernetes.kubernetes.Predicates.onCondition;
import static com.enonic.kubernetes.operator.helpers.PasswordGenerator.getRandomScramble;

/**
 * This operator class sends events on ConfigMap changes
 */
@ApplicationScoped
public class OperatorConfigMapEvent
    extends InformerEventHandler<ConfigMap>
{
    @Inject
    Clients clients;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.configMapInformer() );
    }

    @Override
    protected void onNewAdd( final ConfigMap newCm )
    {
        onCondition( newCm, this::handle, isEnonicManaged() );
    }

    @Override
    public void onUpdate( final ConfigMap oldCm, final ConfigMap newCm )
    {
        onCondition( newCm, this::handle, isEnonicManaged(), dataEquals( oldCm ).negate() );
    }

    @Override
    public void onDelete( final ConfigMap oldCm, final boolean b )
    {
        // Do nothing
    }

    private void handle( final ConfigMap newCm )
    {
        // Build new Event
        Event event = new EventBuilder()
            .editOrNewMetadata()
            .withNamespace( newCm.getMetadata().getNamespace() )
            .withName( newCm.getMetadata().getName() + "." + getRandomScramble( "0123456789abcdefghijklmnopqrstuvwxyz", 16 ) ).endMetadata()
            //            withMessage( "ConfigMap " + newCm.getMetadata().getName() + " updated " )
            .withReason( "ConfigModified" )
            .editOrNewRegarding()
            .withApiVersion( newCm.getApiVersion() )
            .withKind( newCm.getKind() )
            .withName( newCm.getMetadata().getName() )
            .withNamespace( newCm.getMetadata().getNamespace() ).endRegarding().withDeprecatedLastTimestamp( Instant.now().toString() ).withType(
                "Normal" ).build();

        // Send event
        ImmutableK8sCommand.builder().
            action( K8sCommandAction.CREATE ).
            resource( event ).
            wrappedRunnable( () -> clients.k8s().events().v1beta1().events().create( event ) ).
            build().
            run();
    }
}
