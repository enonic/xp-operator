package com.enonic.cloud.operator.common.commands;

import java.util.List;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class CombinedCommand
    implements Command
{
    private final static Logger log = LoggerFactory.getLogger( CombinedCommand.class );

    protected abstract List<Command> command();

    protected abstract String id();

    @SuppressWarnings("unused")
    @Override
    public void execute()
        throws Exception
    {
        for ( int i = 0; i < command().size(); i++ )
        {
            Command c = command().get( i );
            log.info( String.format( "%s: %s", id(), c.toString() ) );
            c.execute();
        }
    }
}
