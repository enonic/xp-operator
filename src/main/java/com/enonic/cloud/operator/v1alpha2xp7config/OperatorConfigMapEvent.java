package com.enonic.cloud.operator.v1alpha2xp7config;

import java.time.Instant;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.commands.ImmutableK8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandAction;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.helpers.PasswordGenerator.getRandomScramble;

/**
 * This operator class sends events on ConfigMap changes
 */
@Singleton
public class OperatorConfigMapEvent
    extends InformerEventHandler<ConfigMap>
{
    @Inject
    Clients clients;

    @Override
    protected void onNewAdd( final ConfigMap newCm )
    {
        if ( managed( newCm ) )
        {
            handle( newCm );
        }
    }

    @Override
    public void onUpdate( final ConfigMap oldCm, final ConfigMap newCm )
    {
        if ( managed( newCm ) )
        {
            if ( !Objects.equals( oldCm.getData(), newCm.getData() ) || !Objects.equals( oldCm.getBinaryData(), newCm.getBinaryData() ) )
            {
                handle( newCm );
            }
        }
    }

    @Override
    public void onDelete( final ConfigMap oldCm, final boolean b )
    {
        // Do nothing
    }

    private void handle( final ConfigMap newCm )
    {
        Event event = new EventBuilder().editOrNewMetadata().
            withNamespace( newCm.getMetadata().getNamespace() ).
            withName( newCm.getMetadata().getName() + "." + getRandomScramble( "0123456789abcdefghijklmnopqrstuvwxyz", 16 ) ).
            endMetadata().
            withMessage( "ConfigMap " + newCm.getMetadata().getName() + " updated " ).
            withReason( "ConfigModified" ).
            editOrNewInvolvedObject().
            withApiVersion( newCm.getApiVersion() ).
            withKind( newCm.getKind() ).
            withName( newCm.getMetadata().getName() ).
            withNamespace( newCm.getMetadata().getNamespace() ).
            endInvolvedObject().
            withLastTimestamp( Instant.now().toString() ).
            withType( "Normal" ).
            build();

        ImmutableK8sCommand.builder().
            action( K8sCommandAction.CREATE ).
            resource( event ).
            wrappedRunnable( () -> clients.k8s().events().create( event ) ).
            build().
            run();
    }

    private boolean managed( final ConfigMap newCm )
    {
        return newCm.getMetadata().getLabels() != null &&
            Objects.equals( newCm.getMetadata().getLabels().get( cfgStr( "operator.charts.values.labelKeys.managed" ) ), "true" );
    }
}
