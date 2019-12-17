package com.enonic.ec.kubernetes.operator.commands.deployments;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteConfigMap;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeletePodDisruptionBudget;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteStatefulSet;
import com.enonic.ec.kubernetes.operator.info.xp7deployment.InfoXp7Deployment;

@Value.Immutable
public abstract class DeleteXpDeploymentNode
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract InfoXp7Deployment info();

    protected abstract String nodeId();

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
            client( defaultClient() ).
            namespace( info().namespaceName() ).
            name( nodeId() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeleteStatefulSet.builder().
            client( defaultClient() ).
            namespace( info().namespaceName() ).
            name( nodeId() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeletePodDisruptionBudget.builder().
            client( defaultClient() ).
            namespace( info().namespaceName() ).
            name( nodeId() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeleteConfigMap.builder().
            client( defaultClient() ).
            namespace( info().namespaceName() ).
            name( nodeId() ).
            build() );
    }
}
