package com.enonic.ec.kubernetes.common.commands;

public interface CombinedCommandBuilder
{
    void addCommands( ImmutableCombinedCommand.Builder commandBuilder );
}
