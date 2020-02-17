package com.enonic.ec.kubernetes.operator.operators.common;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;

public abstract class Operator
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( Operator.class );

    protected void runCommands( String actionId, Consumer<ImmutableCombinedCommand.Builder> commandBuilderConsumer )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( actionId );
        runCommands( commandBuilder, () -> commandBuilderConsumer.accept( commandBuilder ) );
    }

    protected synchronized void runCommands( ImmutableCombinedCommand.Builder commandBuilder, Runnable r )
    {
        r.run();
        try
        {
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            log.error( "Failed running commands", e );
        }
    }
}
