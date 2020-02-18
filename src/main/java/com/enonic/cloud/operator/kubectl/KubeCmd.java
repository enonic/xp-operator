package com.enonic.cloud.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.kubectl.base.ImmutableKubeCommandOptions;
import com.enonic.cloud.operator.kubectl.base.KubeCommand;
import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;
import com.enonic.cloud.operator.kubectl.base.KubeCommandOptions;
import com.enonic.cloud.operator.operators.common.clients.Clients;

@Value.Immutable
public abstract class KubeCmd
{
    protected abstract Clients clients();

    protected abstract Optional<String> namespace();

    protected abstract HasMetadata resource();

    @Value.Default
    protected KubeCommandOptions options()
    {
        return ImmutableKubeCommandOptions.builder().build();
    }

    @SuppressWarnings("WeakerAccess")
    @Value.Derived
    protected KubeCommandBuilder<HasMetadata> cmd()
    {
        return CommandMapper.getCommandClass( clients(), namespace(), resource(), options() );
    }

    public void apply( ImmutableCombinedCommand.Builder commandBuilder )
    {
        Optional<KubeCommand> apply = cmd().apply();
        apply.ifPresent( commandBuilder::addCommand );
    }

    public void delete( ImmutableCombinedCommand.Builder commandBuilder )
    {
        Optional<KubeCommand> delete = cmd().delete();
        delete.ifPresent( commandBuilder::addCommand );
    }
}
