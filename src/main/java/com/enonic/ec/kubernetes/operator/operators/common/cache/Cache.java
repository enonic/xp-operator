package com.enonic.ec.kubernetes.operator.operators.common.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

public abstract class Cache<T extends HasMetadata, L extends KubernetesResourceList<T>>
{
    private final static Logger log = LoggerFactory.getLogger( Cache.class );

    protected static final ExecutorService defaultExecutorService = Executors.newSingleThreadExecutor();

    protected final Map<String, HasMetadata> cache;

    private final List<OnAction<T>> watchers;

    private final ExecutorService executorService;

    Cache(ExecutorService executorService)
    {
        this.executorService = executorService;
        watchers = new LinkedList<>();
        cache = new HashMap<>();
    }

    public void addWatcher( OnAction<T> watcher )
    {
        watchers.add( watcher );
    }

    void startWatcher( FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> filter, Watcher<T> watcher )
    {
        filter.list().getItems().forEach( r -> cache.put( r.getMetadata().getUid(), r ) );
        filter.watch( watcher );
    }

    @SuppressWarnings("unchecked")
    void watcherEventReceived( final Watcher.Action action, final T resource )
    {
        try
        {
            String uid = resource.getMetadata().getUid();
            final Optional<T> oldResource = (Optional<T>) Optional.ofNullable( cache.get( uid ) );
            final Optional<T> newResource = action == Watcher.Action.DELETED ? Optional.empty() : Optional.of( resource );

            if ( oldResource.isPresent() )
            {
                // This is an update
                int knownResourceVersion = Integer.parseInt( oldResource.get().getMetadata().getResourceVersion() );
                int receivedResourceVersion = Integer.parseInt( resource.getMetadata().getResourceVersion() );
                if ( knownResourceVersion > receivedResourceVersion )
                {
                    // This is an old event
                    return;
                }
            }

            log.debug( "Resource " + uid + " " + action + " " + resource.getKind() + ": " + resource.getMetadata().getName() );
            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
            {
                cache.put( uid, resource );
            }
            else if ( action == Watcher.Action.DELETED )
            {
                cache.remove( uid );
            }
            else
            {
                // This should never happen, best just to let kubernetes restart the operator
                log.error(
                    "Received unexpected event " + action + " for " + resource.getMetadata().getUid() + " " + resource.getKind() + ": " +
                        resource.getMetadata().getName() );
                System.exit( -1 );
            }
            runExecutor( action, resource, oldResource, newResource );
        }
        catch ( Exception e )
        {
            // Best just to let kubernetes restart the operator
            log.error( "Something when terribly wrong", e );
            System.exit( -1 );
        }
    }

    private void runExecutor( final Watcher.Action action, final T resource, final Optional<T> oldResource, final Optional<T> newResource )
    {
        executorService.execute( () -> {
            final String actionId = UUID.randomUUID().toString().substring( 0, 8 );
            if ( resource.getMetadata().getNamespace() != null )
            {
                log.info(
                    String.format( "%s: Event in NS '%s': %s '%s' %s", actionId, resource.getMetadata().getNamespace(), resource.getKind(),
                                   resource.getMetadata().getName(), action ) );
            }
            else
            {
                log.info(
                    String.format( "%s: Event: %s '%s' %s", actionId, resource.getKind(), resource.getMetadata().getName(), action ) );
            }
            watchers.forEach( watcher -> watcher.accept( actionId, action, oldResource, newResource ) );
        } );
    }

    void watcherOnClose( final KubernetesClientException cause )
    {
        if ( cause != null )
        {
            // This means the socket closed and we have a problem, best to
            // let kubernetes just restart the operator pod.
            cause.printStackTrace();
            System.exit( -1 );
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<T> getCollection()
    {
        return (Collection<T>) cache.values();
    }

    public Optional<T> get( String namespace, String name )
    {
        return getCollection().stream().
            filter( r -> Objects.equals( namespace, r.getMetadata().getNamespace() ) ).
            filter( r -> name.equals( r.getMetadata().getName() ) ).findFirst();
    }

    public Stream<T> getByNamespace( String namespace )
    {
        return getCollection().stream().filter( r -> Objects.equals( namespace, r.getMetadata().getNamespace() ) );
    }
}
