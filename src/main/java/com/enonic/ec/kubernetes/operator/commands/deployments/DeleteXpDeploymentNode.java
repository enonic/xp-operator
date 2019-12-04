package com.enonic.ec.kubernetes.operator.commands.deployments;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteConfigMap;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeletePodDisruptionBudget;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteStatefulSet;

@Value.Immutable
public abstract class DeleteXpDeploymentNode
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract String namespace();

    protected abstract String nodeName();

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        commandBuilder.addCommand( ImmutableCommandDeleteStatefulSet.builder().
            client( defaultClient() ).
            namespace( namespace() ).
            name( nodeName() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeletePodDisruptionBudget.builder().
            client( defaultClient() ).
            namespace( namespace() ).
            name( nodeName() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeleteConfigMap.builder().
            client( defaultClient() ).
            namespace( namespace() ).
            name( nodeName() ).
            build() );
    }
}
