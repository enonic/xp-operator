package com.enonic.ec.kubernetes.common.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class CombinedKubernetesCommand
    implements Command<Void>
{
    private final static Logger log = LoggerFactory.getLogger( CombinedKubernetesCommand.class );

    protected abstract List<KubernetesCommand> command();

    @Override
    public Void execute()
        throws Exception
    {
        log.info( "Running " + command().size() + " commands" );
        for ( int i = 0; i < command().size(); i++ )
        {
            KubernetesCommand c = command().get( i );
            log.info( String.format( "%2s: %s", i + 1, c.summary() ) );
            c.execute();
        }
        log.info( "Finished" );
        return null;
    }

    @Value.Derived
    public List<KubernetesCommandSummary> summary()
    {
        return command().stream().map( KubernetesCommand::summary ).collect( Collectors.toList() );
    }
}
