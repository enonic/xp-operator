package com.enonic.cloud.operator.operators.common.queues;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;

public class ResourceChangeQueue<R extends HasMetadata>
    extends TimerTask
{
    private final ReentrantLock lock;

    private final Map<String, QueuedAggregator> changeMap;

    private final Duration applyWhenSilentFor;

    public ResourceChangeQueue( Duration applyWhenSilentFor )
    {
        this.applyWhenSilentFor = applyWhenSilentFor;
        this.lock = new ReentrantLock();
        this.changeMap = new ConcurrentHashMap<>();
    }

    public void enqueue( final String actionId, final ResourceChangeAggregator<R> aggregator )
    {
        R resource = aggregator.buildModification();
        lock.lock();
        if ( !changeMap.containsKey( createKey( resource ) ) )
        {
            changeMap.put( createKey( resource ), ImmutableQueuedAggregator.builder().
                actionId( actionId ).
                item( aggregator ).
                build() );
        }
        lock.unlock();

    }

    private String createKey( final R resource )
    {
        return Objects.requireNonNull( resource.getMetadata().getNamespace() ) + ":" +
            Objects.requireNonNull( resource.getMetadata().getName() );
    }

    private void applyAggregator( final QueuedAggregator item )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( item.actionId() );
        item.item().addCommands( commandBuilder );
        try
        {
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            // TODO: What to do?
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    @Override
    public void run()
    {
        if ( changeMap.size() == 0 )
        {
            return;
        }

        lock.lock();

        for ( String key : changeMap.keySet() )
        {
            QueuedAggregator ag = changeMap.get( key );
            if ( ag.instant().plus( applyWhenSilentFor ).isBefore( Instant.now() ) )
            {
                applyAggregator( ag );
                changeMap.remove( key );
            }
        }

        lock.unlock();
    }
}
