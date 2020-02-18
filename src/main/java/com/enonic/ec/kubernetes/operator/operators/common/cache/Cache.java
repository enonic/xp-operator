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

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

public abstract class Cache<T extends HasMetadata, L extends KubernetesResourceList<T>>
    implements Watcher<T>
{
    private final static Logger log = LoggerFactory.getLogger( Cache.class );

    protected static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected final Map<String, T> cache;

    private final List<OnAction<T>> eventListeners;

    private FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> filter;

    protected Cache()
    {
        this.eventListeners = new LinkedList<>();
        this.cache = new HashMap<>();
    }

    protected Cache( final FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> filter )
    {
        this();
        this.filter = filter;
        this.filter.list().getItems().forEach( r -> cache.put( r.getMetadata().getUid(), r ) );
        startWatcher();
    }

    public void addEventListener( OnAction<T> eventListener )
    {
        eventListeners.add( eventListener );
    }

    protected void startWatcher()
    {
        executorService.execute( () -> filter.watch( this ) );
    }

    @Override
    public void eventReceived( final Action action, final T resource )
    {
        try
        {
            String uid = resource.getMetadata().getUid();
            log.debug( "Resource " + uid + " " + action + " " + resource.getKind() + ": " + resource.getMetadata().getName() );

            T oldResource = null;

            if ( cache.containsKey( uid ) )
            {
                oldResource = cache.get( uid );
                int knownResourceVersion = Integer.parseInt( oldResource.getMetadata().getResourceVersion() );
                int receivedResourceVersion = Integer.parseInt( resource.getMetadata().getResourceVersion() );
                log.debug( "Resource " + uid + " version known(" + knownResourceVersion + ") received(" + receivedResourceVersion + ")" );
                if ( knownResourceVersion >= receivedResourceVersion )
                {
                    return;
                }
            }

            if ( action == Watcher.Action.ADDED )
            {
                cache.put( uid, resource );
            }
            else if ( action == Watcher.Action.MODIFIED )
            {
                Preconditions.checkState( oldResource != null );
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
            runExecutor( action, resource, oldResource, action == Watcher.Action.DELETED ? null : resource );
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
            log.warn( "Watcher " + this.getClass().getSimpleName() + " closed with exception: ", cause );
            log.warn( "Restarting watcher..." );
            startWatcher();
        }
    }

    private void runExecutor( final Watcher.Action action, final T resource, final T oldResource, final T newResource )
    {
        executorService.execute( () -> {
            if ( eventListeners.size() == 0 )
            {
                return;
            }

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
            eventListeners.forEach(
                watcher -> watcher.accept( actionId, action, Optional.ofNullable( oldResource ), Optional.ofNullable( newResource ) ) );
        } );
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
