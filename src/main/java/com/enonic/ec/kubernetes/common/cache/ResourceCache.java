package com.enonic.ec.kubernetes.common.cache;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

public class ResourceCache<T extends HasMetadata>
{
    private final Logger log = LoggerFactory.getLogger( ResourceCache.class );

    private final Map<String, T> cache;

    private final List<OnAction<T>> eventListeners;

    private Executor executor;

    protected ResourceCache()
    {
        this.cache = new ConcurrentHashMap<>();
        this.eventListeners = new LinkedList<>();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public List<T> list()
    {
        return cache.entrySet().stream().map( e -> e.getValue() ).collect( Collectors.toList() );
    }

    public T get( String id )
    {
        return cache.get( id );
    }

    public void addWatcher( OnAction<T> event )
    {
        eventListeners.add( event );
    }

    protected void initialize( List<T> initialState )
    {
        initialState.forEach( resource -> {
            cache.put( resource.getMetadata().getUid(), resource );
        } );
    }

    protected void handleEvent( final Watcher.Action action, final T resource )
    {
        try
        {
            String uid = resource.getMetadata().getUid();
            if ( cache.containsKey( uid ) )
            {
                int knownResourceVersion = Integer.parseInt( cache.get( uid ).getMetadata().getResourceVersion() );
                int receivedResourceVersion = Integer.parseInt( resource.getMetadata().getResourceVersion() );
                if ( knownResourceVersion > receivedResourceVersion )
                {
                    return;
                }
            }
            log.info( "Resource " + uid + " " + action + " " + resource.getKind() + ": " + resource.getMetadata().getName() );
            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
            {
                cache.put( uid, resource );
                executor.execute( () -> {
                    eventListeners.forEach( eventListener -> eventListener.accept( action, uid, resource ) );
                } );
            }
            else if ( action == Watcher.Action.DELETED )
            {
                cache.remove( uid );
                executor.execute( () -> {
                    eventListeners.forEach( eventListener -> eventListener.accept( action, uid, resource ) );
                } );
            }
            else
            {
                log.error(
                    "Received unexpected event " + action + " for " + resource.getMetadata().getUid() + " " + resource.getKind() + ": " +
                        resource.getMetadata().getName() );
                System.exit( -1 );
            }
        }
        catch ( Exception e )
        {
            //TODO: Should this be the case?
            e.printStackTrace();
            System.exit( -1 );
        }
    }
}