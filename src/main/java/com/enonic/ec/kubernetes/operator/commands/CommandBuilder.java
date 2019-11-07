package com.enonic.ec.kubernetes.operator.commands;

import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;

public interface CommandBuilder
{
    void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder );
}
