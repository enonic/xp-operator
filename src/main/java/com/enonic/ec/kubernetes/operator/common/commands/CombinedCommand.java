package com.enonic.ec.kubernetes.operator.common.commands;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class CombinedCommand
    implements Command
{
    private final static Logger log = LoggerFactory.getLogger( CombinedCommand.class );

    protected abstract List<Command> command();

    @SuppressWarnings("unused")
    @Override
    public void execute()
        throws Exception
    {
        if ( command().size() == 0 )
        {
            log.debug( "No commands to run" );
            return;
        }
        String uuid = UUID.randomUUID().toString().substring( 0, 8 );
        log.info( String.format( "%s: %s", uuid, "Running " + command().size() + " commands" ) );
        for ( int i = 0; i < command().size(); i++ )
        {
            Command c = command().get( i );
            log.info( String.format( "%s: %s", uuid, c.toString() ) );
            c.execute();
        }
        log.info( String.format( "%s: %s", uuid, "Finished" ) );
    }

}
