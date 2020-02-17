package com.enonic.ec.kubernetes.operator.operators.common;

import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;

public abstract class Operator
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( Operator.class );

    protected synchronized void stallAndRunCommands( Long ms, Runnable r )
    {
        waitSome( ms );
        r.run();
    }

    private void waitSome( Long ms )
    {
        try
        {
            Thread.sleep( ms );
        }
        catch ( InterruptedException e )
        {
            // Just ignore, not a big deal
        }
    }

    protected void runCommands( String cmdId, Consumer<ImmutableCombinedCommand.Builder> commandBuilderConsumer )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( cmdId );
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

    protected void logEvent( Logger log, String id, HasMetadata resource, Watcher.Action action )
    {
        if ( resource.getMetadata().getNamespace() != null )
        {
            log.info( String.format( "%s: Event in NS '%s': %s '%s' %s", id, resource.getMetadata().getNamespace(), resource.getKind(),
                                     resource.getMetadata().getName(), action ) );
        }
        else
        {
            log.info( String.format( "%s: Event: %s '%s' %s", id, resource.getKind(), resource.getMetadata().getName(), action ) );
        }
    }

    protected String createCmdId()
    {
        return UUID.randomUUID().toString().substring( 0, 8 );
    }
}
