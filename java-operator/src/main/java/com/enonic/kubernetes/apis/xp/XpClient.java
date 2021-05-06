package com.enonic.kubernetes.apis.xp;

import com.enonic.kubernetes.apis.xp.service.AppEvent;
import com.enonic.kubernetes.apis.xp.service.AppEventList;
import com.enonic.kubernetes.apis.xp.service.AppEventType;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.apis.xp.service.AppInstallRequest;
import com.enonic.kubernetes.apis.xp.service.AppInstallResponse;
import com.enonic.kubernetes.apis.xp.service.AppKey;
import com.enonic.kubernetes.apis.xp.service.ImmutableAppEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class XpClient
{
    private static final Logger log = LoggerFactory.getLogger( XpClient.class );

    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient restClient;

    private final OkHttpClient sseClient;

    private final EventSource eventSources;

    private final Set<Consumer<AppEvent>> onEventConsumers;

    private final Function<String, Request.Builder> requestBuilder;

    private Boolean clientAlive;

    private final XpClientParams params;

    private final Map<String, AppInfo> appMap;

    private final Runnable onClose;

    private final CountDownLatch onOpenLatch;

    public XpClient( final XpClientParams p, final Runnable onClose )
        throws IOException
    {
        this.onClose = onClose;
        this.appMap = new HashMap<>();
        this.params = p;
        this.clientAlive = false;

        requestBuilder = url -> new Request.Builder().url( params.url() + url );

        onEventConsumers = new HashSet<>();
        onOpenLatch = new CountDownLatch( 1 );

        sseClient = new OkHttpClient().newBuilder()
            .addInterceptor( new XpAuthenticator( params.username(), params.password() ) )
            .connectTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .callTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .readTimeout( 0L, TimeUnit.MILLISECONDS )
            .build();
        restClient = sseClient.newBuilder()
            .readTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .build();


        // Open up SSE
        eventSources = EventSources
            .createFactory( sseClient )
            .newEventSource( requestBuilder.apply( "/app/events" ).build(), new EventSourceListener()
            {
                @Override
                public void onClosed( @NotNull EventSource eventSource )
                {
                    onSSEClosed( eventSource );
                }

                @Override
                public void onEvent( @NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data )
                {
                    onSSEEvent( eventSource, id, type, data );
                }

                @Override
                public void onFailure( @NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response )
                {
                    onSSEFailure( eventSource, t, response );
                }

                @Override
                public void onOpen( @NotNull EventSource eventSource, @NotNull Response response )
                {
                    onSSEOpen( eventSource, response );
                }
            } );

        try {
            onOpenLatch.await( params.timeout(), TimeUnit.MILLISECONDS );
        } catch (InterruptedException e) {
            throw new IOException( String.format(
                "XP: SSE event source timed out for '%s' in NS '%s': %s",
                params.nodeGroup(),
                params.namespace() ) );
        }
        if (!clientAlive) {
            throw new IOException( "Could not create client" );
        }
    }

    private void openLatch()
    {
        if (onOpenLatch.getCount() > 0) {
            onOpenLatch.countDown();
        }
    }

    private void onSSEClosed( @NotNull EventSource eventSource )
    {
        log.debug( String.format(
            "XP: SSE event source closed from '%s' in NS '%s'",
            params.nodeGroup(),
            params.namespace() ) );
        closeClient( null );
    }

    private void onSSEEvent( @NotNull EventSource eventSource, @Nullable String eventId, @Nullable String eventType, @NotNull String eventData )
    {
        log.debug( String.format(
            "XP: SSE event '%s' received from '%s' in NS '%s': %s",
            eventType,
            params.nodeGroup(),
            params.namespace(),
            eventData ) );

        try {
            AppEventType type = AppEventType.fromEventName( eventType );
            switch (type) {
                case LIST:
                    mapper.readValue( eventData, AppEventList.class ).
                        applications().
                        forEach( info -> handleAppInfo( type, info ) );
                    openLatch();
                    break;
                case INSTALLED:
                case STATE:
                    handleAppInfo( type, mapper.readValue( eventData, AppInfo.class ) );
                    break;
                case UNINSTALLED:
                    handleAppUninstall( type, mapper.readValue( eventData, AppKey.class ) );
                    break;
            }
        } catch (Exception e) {
            log.error( String.format(
                "XP: SSE event failed to parse '%s' in NS '%s': %s",
                params.nodeGroup(),
                params.namespace(),
                e.getMessage() ) );
            log.debug( e.getMessage(), e );
        }
    }

    private void onSSEFailure( @NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response )
    {
        log.error( String.format(
            "XP: SSE event source error from '%s' in NS '%s': %s",
            params.nodeGroup(),
            params.namespace(),
            t == null ? null : t.getMessage() ) );
        closeClient( t );
    }

    private synchronized void onSSEOpen( @NotNull EventSource eventSource, @NotNull Response response )
    {
        log.info( String.format(
            "XP: SSE event source opened to '%s' in NS '%s'",
            params.nodeGroup(),
            params.namespace() ) );
        clientAlive = true;
    }

    private synchronized void handleAppInfo( final AppEventType type, final AppInfo info )
    {
        appMap.put( info.key(), info );
        onEventConsumers.forEach( consumer -> consumer.accept( ImmutableAppEvent.
            builder().
            namespace( params.namespace() ).
            nodeGroup( params.nodeGroup() ).
            type( type ).
            key( info.key() ).
            info( info ).
            build() ) );
    }

    private synchronized void handleAppUninstall( final AppEventType type, final AppKey key )
    {
        appMap.remove( key.key() );
        onEventConsumers.forEach( consumer -> consumer.accept( ImmutableAppEvent.
            builder().
            namespace( params.namespace() ).
            nodeGroup( params.nodeGroup() ).
            type( type ).
            key( key.key() ).
            build() ) );
    }

    public synchronized void closeClient( Throwable t )
    {
        if (t != null) {
            log.debug( t.getMessage(), t );
        }

        openLatch();

        if (clientAlive) {
            log.info( String.format(
                "XP: SSE event source closed to '%s' in NS '%s'",
                params.nodeGroup(),
                params.namespace() ) );
            
            eventSources.cancel();
            sseClient.dispatcher().executorService().shutdown();
            restClient.dispatcher().executorService().shutdown();
            clientAlive = false;
            onClose.run();
        }
    }

    private synchronized void assertClient()
        throws IOException
    {
        if (!clientAlive) {
            throw new IOException( String.format(
                "XP: Client not open to '%s' in NS '%s'",
                params.nodeGroup(),
                params.namespace() ) );
        }
    }

    public synchronized void addEventListener( Consumer<AppEvent> eventListener )
        throws IOException
    {
        onEventConsumers.add( eventListener );
        appList().forEach( a -> eventListener.accept( ImmutableAppEvent.
            builder().
            namespace( params.namespace() ).
            nodeGroup( params.nodeGroup() ).
            type( AppEventType.LIST ).
            key( a.key() ).
            info( a ).
            build() ) );
    }

    public synchronized List<AppInfo> appList()
        throws IOException
    {
        assertClient();
        return new ArrayList<>( appMap.values() );
    }

    public AppInstallResponse appInstall( AppInstallRequest req )
        throws IOException
    {
        assertClient();
        Request request = requestBuilder.apply( "/app/installUrl" )
            .post( RequestBody.create( MediaType.parse( "application/json" ), mapper.writeValueAsString( req ) ) )
            .build();
        Response response = restClient.newCall( request ).execute();
        Preconditions.checkState( response.code() == 200, "Response code " + response.code() );
        return mapper.readValue( response.body().bytes(), AppInstallResponse.class );
    }

    private synchronized void appOp( String op, AppKey req )
        throws IOException
    {
        assertClient();

        Request request = requestBuilder.apply( "/app/" + op )
            .post( RequestBody.create( MediaType.parse( "application/json" ), mapper.writeValueAsString( req ) ) )
            .build();
        Response response = restClient.newCall( request ).execute();
        Preconditions.checkState( response.code() == 204, "Response code " + response.code() );
    }

    public void appUninstall( AppKey req )
        throws IOException
    {
        appOp( "uninstall", req );
    }

    public void appStart( AppKey req )
        throws IOException
    {
        appOp( "start", req );
    }

    public void appStop( AppKey req )
        throws IOException
    {
        appOp( "stop", req );
    }
}
