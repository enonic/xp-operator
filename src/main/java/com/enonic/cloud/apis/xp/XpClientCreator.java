package com.enonic.cloud.apis.xp;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.sse.SseEventSource;

import org.immutables.value.Value;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;

import com.enonic.cloud.apis.xp.service.CustomRestHeaderFilter;
import com.enonic.cloud.apis.xp.service.ManagementApi;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Value.Immutable
public abstract class XpClientCreator
{
    private static final ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();

    @Value.Default
    public String serviceName()
    {
        return cfgStr( "operator.charts.values.allNodesKey" );
    }

    public abstract String namespace();

    @Value.Default
    public String username()
    {
        return "su";
    }

    public abstract String password();

    @Value.Derived
    protected String deploymentHost()
    {
        //return String.format( "http://localhost:8001/api/v1/namespaces/%s/services/%s:4848/proxy", namespace(), serviceName() );
        return String.format( "%s.%s.svc.cluster.local", serviceName(), namespace() );
    }

    @Value.Default
    protected String port()
    {
        return "4848";
    }

    @Value.Derived
    protected BasicAuthentication basicAuthentication()
    {
        return new BasicAuthentication( username(), password() );
    }

    @Value.Derived
    protected ResteasyClient client()
    {
        return new ResteasyClientBuilderImpl().httpEngine( engine ).build();
    }

    private ResteasyWebTarget createTarget( UriBuilder uriBuilder )
    {
        ResteasyWebTarget target = client().target( uriBuilder );
        target.register( basicAuthentication() );
        target.register( new CustomRestHeaderFilter( "Host", "localhost" ) );
        target.register( AcceptEncodingGZIPFilter.class );
        target.register( GZIPDecodingInterceptor.class );
        target.register( GZIPEncodingInterceptor.class );
        return target;
    }

    @Value.Derived
    public SseEventSource appsSSE()
    {
        WebTarget target = client().target( UriBuilder.fromPath( "http://" + deploymentHost() + ":" + port() + "/app/events" ) );
        target.register( new CustomRestHeaderFilter( "Accept-Encoding", null ) );
        target.register( basicAuthentication() );
        SseEventSource eventSource = SseEventSource.
            target( target ).
            reconnectingEvery( 60, TimeUnit.SECONDS ).
            build();
        return eventSource;
    }

    @Value.Derived
    public ManagementApi appsApi()
    {
        ResteasyWebTarget target = client().target( UriBuilder.fromPath( "http://" + deploymentHost() + ":" + port() ) );
        target.register( basicAuthentication() );
        target.register( AcceptEncodingGZIPFilter.class );
        target.register( GZIPDecodingInterceptor.class );
        target.register( GZIPEncodingInterceptor.class );
        return target.proxy( ManagementApi.class );
    }
}
