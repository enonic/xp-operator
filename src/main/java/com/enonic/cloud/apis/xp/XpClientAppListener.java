package com.enonic.cloud.apis.xp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.apis.xp.service.AppEvent;
import com.enonic.cloud.apis.xp.service.AppEventList;
import com.enonic.cloud.apis.xp.service.AppEventType;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.apis.xp.service.AppKey;
import com.enonic.cloud.apis.xp.service.ImmutableAppEvent;

public class XpClientAppListener
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( XpClientAppListener.class );

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Set<Consumer<AppEvent>> onEventConsumers;

    private final WebTarget target;

    private final String nodeGroup;

    private final String namespace;

    private final ScheduledExecutorService scheduledExecutor;

    private final int reconnectDelaySeconds;

    private SseEventSource eventSource;

    private Map<String, AppInfo> appMap;

    public XpClientAppListener( WebTarget target, String nodeGroup, String namespace )
    {
        this.onEventConsumers = new HashSet<>();
        this.target = target;
        this.nodeGroup = nodeGroup;
        this.namespace = namespace;
        this.scheduledExecutor = ( (ResteasyWebTarget) target ).getResteasyClient().getScheduledExecutor();
        this.reconnectDelaySeconds = 60;
        this.appMap = new HashMap<>();
        this.eventSource = null;

        // Try to connect
        run();
    }

    @Override
    public synchronized void run()
    {
        appMap = new HashMap<>();
        eventSource = SseEventSource.
            target( target ).
            reconnectingEvery( reconnectDelaySeconds, TimeUnit.SECONDS ).
            build();

        eventSource.register( event -> {
            log.debug( String.format( "XP: SSE event '%s' received from '%s' in NS '%s': %s", event.getName(), nodeGroup, namespace,
                                      event.readData() ) );
            handleEvent( event );
        }, error -> {
            log.error( String.format( "XP: SSE event source error from '%s' in NS '%s': %s", nodeGroup, namespace, error.getMessage() ) );
            log.debug( error.getMessage(), error );
        }, () -> {
            log.warn( String.format( "XP: SSE event source closed from '%s' in NS '%s'", nodeGroup, namespace ) );

            // Schedule reconnect
            scheduledExecutor.schedule( this, reconnectDelaySeconds, TimeUnit.SECONDS );
        } );

        eventSource.open();
    }

    public synchronized void addEventListener( Consumer<AppEvent> eventListener )
    {
        onEventConsumers.add( eventListener );
        appMap.entrySet().forEach( e -> eventListener.accept( ImmutableAppEvent.
            builder().
            namespace( namespace ).
            nodeGroup( nodeGroup ).
            type( AppEventType.LIST ).
            key( e.getKey() ).
            info( e.getValue() ).
            build() ) );
    }

    private void handleEvent( final InboundSseEvent event )
    {
        try
        {
            AppEventType type = AppEventType.fromEventName( event.getName() );
            switch ( type )
            {
                case LIST:
                    mapper.readValue( event.readData(), AppEventList.class ).
                        applications().
                        forEach( info -> handleAppInfo( type, info ) );
                    break;
                case INSTALLED:
                case STATE:
                    handleAppInfo( type, mapper.readValue( event.readData(), AppInfo.class ) );
                    break;
                case UNINSTALLED:
                    handleAppUninstall( type, mapper.readValue( event.readData(), AppKey.class ) );
                    break;
            }
        }
        catch ( Exception e )
        {
            log.error( String.format( "XP: SSE event failed to parse '%s' in NS '%s': %s", nodeGroup, namespace, e.getMessage() ) );
            log.debug( e.getMessage(), e );
        }
    }

    private void handleAppInfo( final AppEventType type, final AppInfo info )
    {
        appMap.put( info.key(), info );
        onEventConsumers.forEach( consumer -> consumer.accept( ImmutableAppEvent.
            builder().
            namespace( namespace ).
            nodeGroup( nodeGroup ).
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
            namespace( namespace ).
            nodeGroup( nodeGroup ).
            type( type ).
            key( key.key() ).
            build() ) );
    }
}
