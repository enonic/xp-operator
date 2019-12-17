package com.enonic.ec.kubernetes.operator.common.commands;

public interface CombinedCommandBuilder
{
    void addCommands( ImmutableCombinedCommand.Builder commandBuilder );
}
