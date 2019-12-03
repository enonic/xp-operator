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
        if ( command().size() == 0 )
        {
            log.debug( "No commands to run" );
            return null;
        }
        log.info( "Running " + command().size() + " commands" );
        for ( int i = 0; i < command().size(); i++ )
        {
            Command c = command().get( i );
            log.info( String.format( "%2s: %s", i + 1, c ) );
            c.execute();
        }
        return null;
    }

}
