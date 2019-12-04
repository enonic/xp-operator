package com.enonic.ec.kubernetes.operator.commands.deployments;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyPvc;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutablePvcSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutableServiceSpecBuilder;

@Value.Immutable
public abstract class CreateXpDeploymentBase
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract String serviceName();

    protected abstract Map<String, String> defaultLabels();

    protected abstract Optional<String> sharedStorageName();

    protected abstract Optional<Quantity> sharedStorageSize();

    protected abstract boolean isClustered();

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Create namespace
        commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( namespace() ).
            build() );

        if ( sharedStorageName().isPresent() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPvc.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( sharedStorageName().get() ).
                labels( defaultLabels() ).
                spec( ImmutablePvcSpecBuilder.builder().
                    size( sharedStorageSize().get() ).
                    addAccessMode( isClustered() ? "ReadWriteMany" : "ReadWriteOnce" ). // TODO: This only works on minikube
                    build().
                    spec() ).
                build() );
        }

        // Create es discovery service
        commandBuilder.addCommand( ImmutableCommandApplyService.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( serviceName() ).
            labels( defaultLabels() ).
            spec( ImmutableServiceSpecBuilder.builder().
                selector( defaultLabels() ).
                putPorts( cfgStr( "operator.deployment.xp.port.es.discovery.name" ),
                          cfgInt( "operator.deployment.xp.port.es.discovery.number" ) ).
                publishNotReadyAddresses( true ).
                build().
                spec() ).
            build() );

        // TODO: Create network policy
    }
}
