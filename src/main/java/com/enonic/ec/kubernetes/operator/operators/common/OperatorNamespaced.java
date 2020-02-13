package com.enonic.ec.kubernetes.operator.operators.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.common.info.Diff;

public abstract class OperatorNamespaced
    extends Operator
{
    private final static Logger log = LoggerFactory.getLogger( OperatorNamespaced.class );

    protected void stallAndRunCommands( Long ms, Consumer<ImmutableCombinedCommand.Builder> c )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();
        stallAndRunCommands( ms, commandBuilder, () -> c.accept( commandBuilder ) );
    }

    protected synchronized void stallAndRunCommands( Long ms, ImmutableCombinedCommand.Builder commandBuilder, Runnable r )
    {
        waitSome( ms );
        runCommands( commandBuilder, r );
    }

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

    protected <R extends HasMetadata, D extends Diff<R>> Optional<ResourceInfoNamespaced<R, D>> getInfo( Watcher.Action action,
                                                                                                         Supplier<ResourceInfoNamespaced<R, D>> s )
    {
        try
        {
            return Optional.of( s.get() );
        }
        catch ( Xp7DeploymentNotFound e )
        {
            if ( action != Watcher.Action.DELETED )
            {
                log.warn( e.getMessage() );
            }
            return Optional.empty();
        }
    }
}
