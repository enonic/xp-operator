package com.enonic.ec.kubernetes.operator.common.commands;

@SuppressWarnings("unused")
public interface CombinedCommandBuilder
{
    void addCommands( ImmutableCombinedCommand.Builder commandBuilder );
}
