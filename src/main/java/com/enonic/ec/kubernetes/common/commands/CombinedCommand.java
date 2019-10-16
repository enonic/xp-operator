package com.enonic.ec.kubernetes.common.commands;

import java.util.List;

import org.immutables.value.Value;

@Value.Immutable
public abstract class CombinedCommand
    implements Command<Void>
{
    protected abstract List<Command> command();

    @Override
    public Void execute()
        throws Exception
    {
        for ( Command c : command() )
        {
            c.execute();
        }
        return null;
    }

}
