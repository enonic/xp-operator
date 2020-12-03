package com.enonic.cloud.operator.helpers;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;

import static com.enonic.cloud.kubernetes.Comparators.namespaceAndName;
import static com.enonic.cloud.kubernetes.Predicates.isDeleted;
import static com.enonic.cloud.kubernetes.Predicates.olderThan;


public abstract class HandlerStatus<R extends HasMetadata, S>
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( HandlerStatus.class );

    @ConfigProperty(name = "operator.tasks.status.delay")
    long delay;

    @Override
    public void run()
    {
        preRun();
        informerSearcher().stream().
            filter( isDeleted().negate() ).
            filter( olderThan( 10 ) ).
            sorted( namespaceAndName() ).
            forEach( this::handle );
    }

    public void handle( final R resource )
    {
        // Get old status info
        S oldStatus = getStatus( resource );
        int oldStatusHashCode = oldStatus != null ? oldStatus.hashCode() : 0;

        // Get new status
        S newStatus = pollStatus( resource );
        if ( oldStatusHashCode != newStatus.hashCode() )
        {
            // Create status update
            log.debug( String.format( "Updating status of %s '%s'", resource.getKind(), resource.getMetadata().getName() ) );
            K8sLogHelper.logDoneable( updateStatus( resource, newStatus ) );
        }
    }

    protected void preRun()
    {
        // Do nothing
    }

    protected abstract InformerSearcher<R> informerSearcher();

    protected abstract S getStatus( final R resource );

    protected abstract Doneable<R> updateStatus( final R resource, final S newStatus );

    protected abstract S pollStatus( final R resource );
}
