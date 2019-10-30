package com.enonic.ec.kubernetes.operator.commands;

import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutablePvcSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyPvc;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyService;

@Value.Immutable
public abstract class CreateXpDeploymentBase
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract Map<String, String> defaultLabels();

    protected abstract String esDiscoveryService();

    protected abstract String blobStorageName();

    protected abstract Quantity blobStorageSize();

    protected abstract String snapshotsStorageName();

    protected abstract Quantity snapshotsStorageSize();

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        // Create namespace
        commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( namespace() ).
            build() );

        // Create blob storage
        commandBuilder.addCommand( ImmutableCommandApplyPvc.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( blobStorageName() ).
            labels( defaultLabels() ).
            spec( ImmutablePvcSpecBuilder.builder().
                size( blobStorageSize() ).
                addAccessMode( "ReadWriteMany" ).
                build().
                spec() ).
            build() );

        // Create snapshots storage
        commandBuilder.addCommand( ImmutableCommandApplyPvc.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( snapshotsStorageName() ).
            labels( defaultLabels() ).
            spec( ImmutablePvcSpecBuilder.builder().
                size( snapshotsStorageSize() ).
                addAccessMode( "ReadWriteMany" ).
                build().
                spec() ).
            build() );

        // Create es discovery service
        commandBuilder.addCommand( ImmutableCommandApplyService.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( esDiscoveryService() ).
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
