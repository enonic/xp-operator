package com.enonic.ec.kubernetes.common.cache;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

import com.enonic.ec.kubernetes.common.Configuration;

public class ResourceCache<T extends HasMetadata>
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( ResourceCache.class );

    private final Map<String, T> cache;

    private final List<OnAction<T>> eventListeners;

    private final Executor executor;

    protected ResourceCache()
    {
        this.cache = new ConcurrentHashMap<>();
        this.eventListeners = new LinkedList<>();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Stream<T> stream()
    {
        return cache.values().stream();
    }

    public Optional<T> get( String id )
    {
        return Optional.ofNullable( cache.get( id ) );
    }

    public void addWatcher( OnAction<T> event )
    {
        eventListeners.add( event );
    }

    public void initialize( List<T> initialState )
    {
        initialState.forEach( resource -> cache.put( resource.getMetadata().getUid(), resource ) );
    }

    protected void handleEvent( final Watcher.Action action, final T resource )
    {
        try
        {
            String uid = resource.getMetadata().getUid();
            final Optional<T> oldResource = Optional.ofNullable( cache.get( uid ) );

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
                executor.execute( () -> eventListeners.forEach(
                    eventListener -> eventListener.accept( action, uid, oldResource, Optional.of( resource ) ) ) );
            }
            else if ( action == Watcher.Action.DELETED )
            {
                cache.remove( uid );
                executor.execute( () -> eventListeners.forEach(
                    eventListener -> eventListener.accept( action, uid, Optional.of( resource ), Optional.empty() ) ) );
            }
            else
            {
                // This should never happen, best just to let kubernetes restart the operator
                log.error(
                    "Received unexpected event " + action + " for " + resource.getMetadata().getUid() + " " + resource.getKind() + ": " +
                        resource.getMetadata().getName() );
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
}