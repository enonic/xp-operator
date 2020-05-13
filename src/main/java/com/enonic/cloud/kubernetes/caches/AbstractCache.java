package com.enonic.cloud.kubernetes.caches;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractCache<R extends HasMetadata, L extends KubernetesResourceList<R>>
    implements Watcher<R>
{
    private final static Logger log = LoggerFactory.getLogger( AbstractCache.class );

    private final static Map<Class, String> singletonCheck = new ConcurrentHashMap<>();

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected final Map<String, R> cache;

    private final List<ResourceEventHandler<R>> eventListeners;

    private final FilterWatchListDeletable<R, L, Boolean, Watch, Watcher<R>> filter;

    protected AbstractCache()
    {
        this.eventListeners = null;
        this.filter = null;
        this.cache = new HashMap<>();
    }

    protected AbstractCache( final FilterWatchListDeletable<R, L, Boolean, Watch, Watcher<R>> filter )
    {
        Preconditions.checkState( !singletonCheck.containsKey( this.getClass() ),
                                  String.format( "There is already %s instantiated", this.getClass().getSimpleName() ) );
        singletonCheck.put( this.getClass(), this.getClass().getSimpleName() );

        this.eventListeners = new LinkedList<>();
        this.cache = new ConcurrentHashMap<>();
        this.filter = filter;
        this.filter.list().getItems().forEach( r -> cache.put( r.getMetadata().getUid(), r ) );
        startWatcher();
    }

    public void addEventListener( ResourceEventHandler<R> eventListener )
    {
        eventListeners.add( eventListener );
    }

    private void startWatcher()
    {
        executorService.execute( () -> filter.watch( this ) );
    }

    @Override
    public void eventReceived( final Action action, final R resource )
    {
        executorService.execute( () -> {
            try
            {
                String uid = resource.getMetadata().getUid();
                log.debug( "Resource " + uid + " " + action + " " + resource.getKind() + ": " + resource.getMetadata().getName() );

                final R oldResource = cache.get( uid );

                if ( oldResource != null )
                {
                    int knownResourceVersion = Integer.parseInt( oldResource.getMetadata().getResourceVersion() );
                    int receivedResourceVersion = Integer.parseInt( resource.getMetadata().getResourceVersion() );
                    log.debug(
                        "Resource " + uid + " version known(" + knownResourceVersion + ") received(" + receivedResourceVersion + ")" );
                    if ( knownResourceVersion >= receivedResourceVersion )
                    {
                        return;
                    }
                }

                if ( action == Action.ADDED )
                {
                    cache.put( uid, resource );
                    eventListeners.forEach( l -> doTryListener( () -> l.onAdd( resource ) ) );
                }
                else if ( action == Action.MODIFIED )
                {
                    cache.put( uid, resource );
                    Preconditions.checkState( oldResource != null, "Old resource should not be null" );
                    eventListeners.forEach( l -> doTryListener( () -> l.onUpdate( oldResource, resource ) ) );
                }
                else if ( action == Action.DELETED )
                {
                    cache.remove( uid );
                    eventListeners.forEach(
                        l -> doTryListener( () -> l.onDelete( resource, !resource.getMetadata().getFinalizers().isEmpty() ) ) );
                }
                else
                {
                    throw new Exception(
                        "Received unexpected event " + action + " for " + resource.getMetadata().getUid() + " " + resource.getKind() +
                            ": " + resource.getMetadata().getName() );
                }
            }
            catch ( Exception e )
            {
                // Best just to let kubernetes restart the operator
                log.error( "Something when terribly wrong", e );
                System.exit( -1 );
            }
        } );
    }

    protected void doTryListener( Runnable runnable )
    {
        try
        {
            runnable.run();
        }
        catch ( Exception e )
        {
            log.error( "Exception when calling listener", e );
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

    public Stream<R> getStream()
    {
        return cache.values().stream();
    }

    public Stream<R> get( @NotNull String namespace )
    {
        return getStream().
            filter( r -> Objects.equals( r.getMetadata().getNamespace(), namespace ) );
    }

    public Optional<R> get( String namespace, @NotNull String name )
    {
        return getStream().
            filter( r -> namespace == null || namespace.equals( r.getMetadata().getNamespace() ) ).
            filter( r -> name.equals( r.getMetadata().getName() ) ).
            findFirst();
    }

    public Optional<R> get( R resource )
    {
        return Optional.ofNullable( cache.get( resource.getMetadata().getUid() ) );
    }
}
