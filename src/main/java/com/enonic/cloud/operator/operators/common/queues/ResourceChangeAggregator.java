package com.enonic.cloud.operator.operators.common.queues;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.commands.CombinedCommandBuilder;
import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.kubectl.ImmutableKubeCmd;
import com.enonic.cloud.operator.kubectl.KubeCmd;
import com.enonic.cloud.operator.operators.common.clients.Clients;

public abstract class ResourceChangeAggregator<T extends HasMetadata>
    implements CombinedCommandBuilder
{
    public abstract Clients clients();

    protected abstract T buildModification();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        T resource = buildModification();
        KubeCmd cmd = ImmutableKubeCmd.builder().
            clients( clients() ).
            namespace( resource.getMetadata().getNamespace() ).
            resource( resource ).
            build();
        cmd.apply( commandBuilder );
    }
}
