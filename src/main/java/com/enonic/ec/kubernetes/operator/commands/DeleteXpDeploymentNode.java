package com.enonic.ec.kubernetes.operator.commands;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteConfigMap;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeletePodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteStatefulSet;

@Value.Immutable
public abstract class DeleteXpDeploymentNode
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract String namespace();

    protected abstract String nodeName();

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
//        commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
//            client( defaultClient() ).
//            namespace( namespace() ).
//            name( serviceName() ).
//            build() );

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