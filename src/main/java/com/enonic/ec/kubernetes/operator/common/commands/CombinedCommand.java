package com.enonic.ec.kubernetes.operator.common.commands;

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
        log.debug( "Running " + command().size() + " commands" );
        for ( int i = 0; i < command().size(); i++ )
        {
            Command c = command().get( i );
            log.info( c.toString() );
            c.execute();
        }
        return null;
    }

}
