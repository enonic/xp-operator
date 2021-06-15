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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class XpClient
    extends EventSourceListener
{
    private static final Logger log = LoggerFactory.getLogger( XpClient.class );

    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient restClient;

    private final OkHttpClient sseClient;

    private final EventSource eventSources;

    private final Set<Consumer<AppEvent>> onEventConsumers;

    private final Function<String, Request.Builder> requestBuilder;

    private final XpClientParams params;

    private final Map<String, AppInfo> appMap;

    private final Runnable onClose;

    private final CountDownLatch onOpenLatch;

    public XpClient( final XpClientParams p, final Runnable onClose )
    {
        log.debug( String.format( "XP: Creating client for %s", p.url() ) );
        this.onClose = onClose;
        this.appMap = new ConcurrentHashMap<>();
        this.params = p;

        requestBuilder = url -> new Request.Builder().url( params.url() + url );

        onEventConsumers = new HashSet<>();
        onOpenLatch = new CountDownLatch( 1 );

        sseClient = new OkHttpClient().newBuilder()
            .addInterceptor( new XpAuthenticator( params.username(), params.password() ) )
            .connectTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .callTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .readTimeout( 0L, TimeUnit.MILLISECONDS )
            .build();

        restClient = new OkHttpClient().newBuilder()
            .addInterceptor( new XpAuthenticator( params.username(), params.password() ) )
            .connectTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .callTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .readTimeout( params.timeout(), TimeUnit.MILLISECONDS )
            .build();

        // Open up SSE
        eventSources = EventSources
            .createFactory( sseClient )
            .newEventSource( requestBuilder.apply( "/app/events" ).build(), this );
    }

    public void waitForConnection( final long timeout )
        throws XpClientException
    {
        try {
            onOpenLatch.await( timeout, TimeUnit.MILLISECONDS );
        } catch (InterruptedException e) {
            throw new XpClientException( String.format( "Timed out waiting for SSE connection on ''", params.url() ) );
        }
    }

    private void openLatch()
    {
        if (onOpenLatch.getCount() > 0) {
            onOpenLatch.countDown();
        }
    }

    @Override
    public void onClosed( @NotNull EventSource eventSource )
    {
        log.debug( String.format(
            "XP: SSE event source closed from '%s' in NS '%s'",
            params.nodeGroup(),
            params.namespace() ) );
        closeClient( null );
    }

    @Override
    public void onEvent( @NotNull EventSource eventSource, @Nullable String id, @Nullable String eventType, @NotNull String eventData )
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

    @Override
    public void onFailure( @NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response )
    {
        log.error( String.format(
            "XP: SSE event source error from '%s' in NS '%s': %s",
            params.nodeGroup(),
            params.namespace(),
            t == null ? null : t.getMessage() ) );
        closeClient( t );
    }

    @Override
    public void onOpen( @NotNull EventSource eventSource, @NotNull Response response )
    {
        log.info( String.format(
            "XP: SSE event source opened to '%s' in NS '%s'",
            params.nodeGroup(),
            params.namespace() ) );
    }

    private void handleAppInfo( final AppEventType type, final AppInfo info )
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

    private void handleAppUninstall( final AppEventType type, final AppKey key )
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

    public void closeClient( Throwable t )
    {
        if (t != null) {
            log.debug( t.getMessage(), t );
        }

        log.warn( String.format(
            "XP: SSE event source closed to '%s' in NS '%s'",
            params.nodeGroup(),
            params.namespace() ) );

        eventSources.cancel();
        sseClient.dispatcher().executorService().shutdown();
        restClient.dispatcher().executorService().shutdown();
        onClose.run();
    }

    public void addEventListener( Consumer<AppEvent> eventListener )
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

    public List<AppInfo> appList()
    {
        return new ArrayList<>( appMap.values() );
    }

    public AppInstallResponse appInstall( AppInstallRequest req )
        throws XpClientException
    {
        try {
            Request request = requestBuilder.apply( "/app/installUrl" )
                .post( RequestBody.create( MediaType.parse( "application/json" ), mapper.writeValueAsString( req ) ) )
                .build();
            Response response = restClient.newCall( request ).execute();
            Preconditions.checkState( response.code() == 200, "Response code " + response.code() );
            return mapper.readValue( response.body().bytes(), AppInstallResponse.class );
        } catch (IOException e) {
            throw new XpClientException( String.format( "Failed installing app on '%s'", params.url() ), e );
        }
    }

    private void appOp( String op, AppKey req )
        throws IOException
    {
        Request request = requestBuilder.apply( "/app/" + op )
            .post( RequestBody.create( MediaType.parse( "application/json" ), mapper.writeValueAsString( req ) ) )
            .build();
        Response response = restClient.newCall( request ).execute();
        Preconditions.checkState( response.code() == 204, "Response code " + response.code() );
    }

    public void appUninstall( AppKey req )
        throws XpClientException
    {
        try {
            appOp( "uninstall", req );
        } catch (IOException e) {
            throw new XpClientException( String.format( "Failed uninstalling app on '%s'", params.url() ), e );
        }
    }

    public void appStart( AppKey req )
        throws XpClientException
    {
        try {
            appOp( "start", req );
        } catch (IOException e) {
            throw new XpClientException( String.format( "Failed starting app on '%s'", params.url() ), e );
        }
    }

    public void appStop( AppKey req )
        throws XpClientException
    {
        try {
            appOp( "stop", req );
        } catch (IOException e) {
            throw new XpClientException( String.format( "Failed stopping app on '%s'", params.url() ), e );
        }
    }
}
