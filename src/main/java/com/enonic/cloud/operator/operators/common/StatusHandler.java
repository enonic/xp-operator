package com.enonic.cloud.operator.operators.common;

import java.util.Objects;
import java.util.TimerTask;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.crd.CrdStatus;
import com.enonic.cloud.operator.crd.HasStatus;
import com.enonic.cloud.operator.kubectl.ImmutableKubeCmd;
import com.enonic.cloud.operator.kubectl.base.ImmutableKubeCommandOptions;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;

public abstract class StatusHandler<F, S extends CrdStatus<F>, H extends HasStatus<F, S>>
    extends TimerTask
{
    private static final Logger log = LoggerFactory.getLogger( StatusHandler.class );

    public abstract Caches caches();

    public abstract Clients clients();

    public abstract String actionId();

    protected abstract Stream<H> getResourcesToUpdate( Caches caches );

    protected abstract S createDefaultStatus( final H r );

    protected abstract S createNewStatus( final Caches caches, final H r, final S oldStatus );

    @Override
    public void run()
    {
        getResourcesToUpdate( caches() ).forEach( this::updateStatus );
    }

    public void updateStatus( final H r )
    {
        log.debug( String.format( "Updating status for %s '%s' in NS '%s'", r.getKind(), r.getMetadata().getName(),
                                  r.getMetadata().getNamespace() ) );
        S oldStatus = r.getStatus();
        if ( oldStatus == null )
        {
            oldStatus = createDefaultStatus( r );
        }
        S newStatus = createNewStatus( caches(), r, oldStatus );
        if ( !Objects.equals( r.getStatus(), newStatus ) )
        {
            r.setStatus( newStatus );
            updateResource( r );
        }
    }

    private void updateResource( final H r )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( actionId() );
        ImmutableKubeCmd.builder().
            clients( clients() ).
            namespace( r.getMetadata().getNamespace() ).
            resource( r ).
            options( ImmutableKubeCommandOptions.builder().
                alwaysUpdate( true ).
                build() ).
            build().
            apply( commandBuilder );

        try
        {
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            log.error( String.format( "Failed updating %s '%s' status in NS '%s'", r.getKind(), r.getMetadata().getName(),
                                      r.getMetadata().getNamespace() ), e );
        }
    }
}
