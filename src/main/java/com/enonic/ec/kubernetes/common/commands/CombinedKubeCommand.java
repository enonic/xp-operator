package com.enonic.ec.kubernetes.common.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class CombinedKubeCommand
    implements Command<Void>
{
    private final static Logger log = LoggerFactory.getLogger( CombinedKubeCommand.class );

    protected abstract List<KubeCommand> command();

    @Override
    public Void execute()
        throws Exception
    {
        log.info( "Running " + command().size() + " commands" );
        for ( int i = 0; i < command().size(); i++ )
        {
            KubeCommand c = command().get( i );
            log.info( String.format( "%2s: %s", i + 1, c.summary() ) );
            c.execute();
        }
        return null;
    }

    @Value.Derived
    public List<KubeCommandSummary> summary()
    {
        return command().stream().map( c -> c.summary() ).collect( Collectors.toList() );
    }

}
