package com.enonic.cloud.operator.operators.common;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;

public abstract class Operator
{
    private final static Logger log = LoggerFactory.getLogger( Operator.class );

    protected void runCommands( String actionId, Consumer<ImmutableCombinedCommand.Builder> commandBuilderConsumer )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( actionId );
        runCommands( commandBuilder, () -> commandBuilderConsumer.accept( commandBuilder ) );
    }

    @SuppressWarnings("WeakerAccess")
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
