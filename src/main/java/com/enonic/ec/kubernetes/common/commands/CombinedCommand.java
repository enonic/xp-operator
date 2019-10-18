package com.enonic.ec.kubernetes.common.commands;

import java.util.List;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class CombinedCommand
    implements Command<Void>
{
    private final static Logger log = LoggerFactory.getLogger( CombinedCommand.class );

    protected abstract List<Command> command();

    @Override
    public Void execute()
        throws Exception
    {
        log.debug( "Running " + command().size() + " commands" );
        for ( Command c : command() )
        {
            c.execute();
        }
        return null;
    }

}
