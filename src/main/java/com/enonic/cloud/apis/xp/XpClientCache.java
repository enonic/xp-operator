package com.enonic.cloud.apis.xp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.sse.SseEventSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.cloud.apis.xp.service.AppEvent;
import com.enonic.cloud.apis.xp.service.AppEventList;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.AppKey;
import com.enonic.cloud.apis.xp.service.ImmutableAppEvent;
import com.enonic.cloud.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.cloud.apis.xp.service.ImmutableAppKey;
import com.enonic.cloud.kubernetes.Clients;

@Singleton
public class XpClientCache
    extends CacheLoader<String, XpClientCreator>
{
    private static final Logger log = LoggerFactory.getLogger( XpClientCache.class );

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final BaseEncoding baseEncoding = BaseEncoding.base64();

    private final KubernetesClient client;

    private final LoadingCache<String, XpClientCreator> clientCreatorCache;

    @Inject
    public XpClientCache( final Clients clients )
    {
        this.client = clients.k8s();
        clientCreatorCache = CacheBuilder.newBuilder().
            maximumSize( 1000 ).
            expireAfterWrite( 10, TimeUnit.MINUTES ).
            build( this );
    }

    public void invalidateCache( String namespace )
    {
        clientCreatorCache.invalidate( namespace );
    }

    @SuppressWarnings("WeakerAccess")
    public void addEventListener( final String namespace, final Consumer<AppEvent> eventConsumer )
    {
        SseEventSource msgEventSource = SseEventSource.target( getCreator( namespace ).sseTarget() ).build();
        try (SseEventSource eventSource = msgEventSource)
        {
            eventSource.register( event -> {
                switch ( event.getName() )
                {
                    case "list":
                        event.
                            readData( AppEventList.class ).
                            applications().
                            forEach( e -> eventConsumer.accept( ImmutableAppEvent.builder().info( e ).build() ) );
                    case "installed":
                    case "state":
                        eventConsumer.accept( ImmutableAppEvent.builder().info( event.readData( AppInfo.class ) ).build() );
                    case "uninstalled":
                        eventConsumer.accept( ImmutableAppEvent.builder().uninstall( event.readData( AppKey.class ) ).build() );
                }
            }, e -> log.error( "Error recieving SSE event", e ) );
            eventSource.open();
        }
        catch ( Exception e )
        {
            log.error( String.format( "SSE connection with XP failed in NS '%s'", namespace ), e );
        }
    }

    @SuppressWarnings("WeakerAccess")
    public AppInfo install( final String namespace, final String url )
    {
        AppInstallResponse res =
            getCreator( namespace ).managementApi().appInstall( ImmutableAppInstallRequest.builder().url( url ).build() );
        if ( res.failure() == null )
        {
            log( namespace, "INSTALL app", res.applicationInstalledJson().application().key() );
        }
        else
        {
            throw new RuntimeException( res.failure() );
        }
        return res.applicationInstalledJson().application();
    }

    @SuppressWarnings("WeakerAccess")
    public void uninstall( final String namespace, final String key )
    {
        log( namespace, "UNINSTALL app", key );
        getCreator( namespace ).managementApi().appUninstall( ImmutableAppKey.builder().key( key ).build() );
    }

    public void start( final String namespace, final String key )
    {
        log( namespace, "START app", key );
        getCreator( namespace ).managementApi().appStart( ImmutableAppKey.builder().key( key ).build() );
    }

    public void stop( final String namespace, final String key )
    {
        log( namespace, "STOP app", key );
        getCreator( namespace ).managementApi().appStop( ImmutableAppKey.builder().key( key ).build() );
    }

    private void log( String namespace, String action, String key )
    {
        log.info( String.format( "XP: %s %s in NS '%s'", action, key, namespace ) );
    }

    private XpClientCreator getCreator( String namespace )
    {
        try
        {
            return clientCreatorCache.get( namespace );
        }
        catch ( ExecutionException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public XpClientCreator load( final String namespace )
    {
        Secret secret = client.secrets().
            inNamespace( namespace ).
            withName( "su" ).
            get();
        Preconditions.checkState( secret != null, "Cannot find 'su' secret" );
        Preconditions.checkState( secret.getData().containsKey( "pass" ), "Cannot find key 'pass' in 'su' secret" );

        return ImmutableXpClientCreator.builder().
            namespace( namespace ).
            password( new String( baseEncoding.decode( secret.getData().get( "pass" ) ) ) ).
            build();
    }
}
