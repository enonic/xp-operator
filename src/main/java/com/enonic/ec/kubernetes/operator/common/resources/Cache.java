package com.enonic.ec.kubernetes.operator.common.resources;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

public abstract class Cache<T extends HasMetadata>
{
    private final static Logger log = LoggerFactory.getLogger( Cache.class );

    private final Executor executor;

    private final List<OnAction<T>> watchers;

    protected final Map<String, HasMetadata> cache;

    public Cache()
    {

        executor = Executors.newSingleThreadExecutor();
        watchers = new LinkedList<>();
        cache = new HashMap<>();
    }

    public void addWatcher( OnAction<T> watcher )
    {
        watchers.add( watcher );
    }

    @SuppressWarnings("unchecked")
    void onStartup( @Observes StartupEvent _ev )
    {
        ((FilterWatchListDeletable<? extends HasMetadata, KubernetesResourceList<? extends HasMetadata>, Boolean, Watch, Watcher<? extends HasMetadata>>)filter()).list().getItems().forEach( r -> cache.put( r.getMetadata().getUid(), r ) );
        ((FilterWatchListDeletable<? extends HasMetadata, KubernetesResourceList<? extends HasMetadata>, Boolean, Watch, Watcher<? extends HasMetadata>>)filter()).watch( new Watcher<>()
        {
            @SuppressWarnings("unchecked")
            @Override
            public void eventReceived( final Action action, final HasMetadata resource )
            {
                if ( !getResourceClass().isInstance( resource ) )
                {
                    return;
                }
                try
                {
                    String uid = resource.getMetadata().getUid();
                    final Optional<T> oldResource = (Optional<T>) Optional.ofNullable( cache.get( uid ) );
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
                        watchers.forEach(
                            watcher -> executor.execute( () -> watcher.accept( action, uid, oldResource, Optional.of( (T) resource ) ) ) );
                    }
                    else if ( action == Watcher.Action.DELETED )
                    {
                        cache.remove( uid );
                        watchers.forEach(
                            watcher -> executor.execute( () -> watcher.accept( action, uid, oldResource, Optional.of( (T) resource ) ) ) );
                    }
                    else
                    {
                        // This should never happen, best just to let kubernetes restart the operator
                        log.error(
                            "Received unexpected event " + action + " for " + resource.getMetadata().getUid() + " " + resource.getKind() +
                                ": " + resource.getMetadata().getName() );
                        System.exit( -1 );
                    }
                }
                catch ( Exception e )
                {
                    // Best just to let kubernetes restart the operator
                    log.error( "Something when terribly wrong", e );
                    System.exit( -1 );
                }
            }

            @Override
            public void onClose( final KubernetesClientException cause )
            {
                if ( cause != null )
                {
                    // This means the socket closed and we have a problem, best to
                    // let kubernetes just restart the operator pod.
                    cause.printStackTrace();
                    System.exit( -1 );
                }
            }
        } );
    }

    @SuppressWarnings("unchecked")
    private Collection<T> getCollection()
    {
        return (Collection<T>) cache.values();
    }

    public Optional<T> get( String namespace, String name )
    {
        return getCollection().stream().
            filter( r -> namespace == null || namespace.equals( r.getMetadata().getNamespace() ) ).
            filter( r -> name.equals( r.getMetadata().getName() ) ).findFirst();
    }

    public Stream<T> getByNamespace( String namespace )
    {
        return getCollection().stream().filter( r -> namespace.equals( r ) );
    }

    protected abstract Class<T> getResourceClass();

    protected abstract FilterWatchListDeletable filter();
}
