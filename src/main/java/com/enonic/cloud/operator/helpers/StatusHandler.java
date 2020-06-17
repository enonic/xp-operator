package com.enonic.cloud.operator.helpers;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.operator.functions.HasMetadataSorterImpl;
import com.enonic.cloud.operator.functions.StatusUpdateFilterImpl;


public abstract class StatusHandler<R extends HasMetadata, S>
    implements Runnable
{
    @Inject
    TaskRunner taskRunner;

    Predicate<HasMetadata> statusFilter;

    Comparator<HasMetadata> sorter;

    public void onStartup( @Observes StartupEvent _ev )
    {
        statusFilter = StatusUpdateFilterImpl.builder().build();
        sorter = HasMetadataSorterImpl.builder().build();
        taskRunner.scheduleAtFixedRate( this, initialDelayMs(), periodMs(), TimeUnit.MILLISECONDS );
    }

    @Override
    public void run()
    {
        preRun();
        informerSearcher().
            getStream().
            filter( statusFilter ).
            sorted( sorter ).
            forEach( this::handle );
    }

    private void handle( final R resource )
    {
        // Get old status info
        S oldStatus = getStatus( resource );
        int oldStatusHashCode = oldStatus != null ? oldStatus.hashCode() : 0;

        // Get new status
        S newStatus = pollStatus( resource );
        if ( oldStatusHashCode != newStatus.hashCode() )
        {
            // Create status update
            K8sLogHelper.logDoneable( updateStatus( resource, newStatus ) );
        }
    }

    protected Long periodMs()
    {
        return 3000L;
    }

    protected Long initialDelayMs()
    {
        return 5000L;
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
