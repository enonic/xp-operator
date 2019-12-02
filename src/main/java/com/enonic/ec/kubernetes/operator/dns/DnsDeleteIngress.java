package com.enonic.ec.kubernetes.operator.dns;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;

@Value.Immutable
public abstract class DnsDeleteIngress
    extends DnsCommand
    implements CombinedCommandBuilder
{
    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        if ( !areManaged() )
        {
            return;
        }
        records().forEach( r -> delete( commandBuilder, r ) );
    }
}
