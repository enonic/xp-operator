package com.enonic.ec.kubernetes.operator;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.info.XpDeploymentNotFound;

public class OperatorNamespaced
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( OperatorNamespaced.class );

    protected synchronized void stallAndRunCommands( Consumer<ImmutableCombinedCommand.Builder> c )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();
        stallAndRunCommands( commandBuilder, () -> c.accept( commandBuilder ) );
    }

    protected synchronized void stallAndRunCommands( ImmutableCombinedCommand.Builder commandBuilder, Runnable r )
    {
        waitSome();
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

    protected void waitSome()
    {
        try
        {
            Thread.sleep( 500 );
        }
        catch ( InterruptedException e )
        {
            // Do nothing
        }
    }

    protected <R extends HasMetadata, D extends Diff<R>> Optional<ResourceInfoNamespaced<R, D>> getInfo( Watcher.Action action,
                                                                                                         Supplier<ResourceInfoNamespaced<R, D>> s )
    {
        try
        {
            return Optional.of( s.get() );
        }
        catch ( XpDeploymentNotFound e )
        {
            if ( action != Watcher.Action.DELETED )
            {
                log.warn( e.getMessage() );
            }
            return Optional.empty();
        }
    }
}
