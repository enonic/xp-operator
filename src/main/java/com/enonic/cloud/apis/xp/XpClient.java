package com.enonic.cloud.apis.xp;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

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

@Value.Immutable
public abstract class XpClient
{
    private static final ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();

    protected abstract String nodeGroup();

    public abstract String namespace();

    @Value.Default
    public String username()
    {
        return "su";
    }

    public abstract String password();

    protected UriBuilder uri()
    {
        return uri( "" );
    }

    protected UriBuilder uri( final String path )
    {
        return UriBuilder.fromPath( String.format( "http://%s.%s.svc.cluster.local:4848%s", nodeGroup(), namespace(), path ) );
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

    protected XpClientAppListener appsSSE()
    {
        WebTarget target = client().target( uri( "/app/events" ) );
        target.register( new CustomRestHeaderFilter( "Accept-Encoding", null ) );
        target.register( basicAuthentication() );
        return new XpClientAppListener( target, nodeGroup(), namespace() );
    }

    @Value.Derived
    public ManagementApi appsApi()
    {
        ResteasyWebTarget target = client().target( uri() );
        target.register( basicAuthentication() );
        target.register( AcceptEncodingGZIPFilter.class );
        target.register( GZIPDecodingInterceptor.class );
        target.register( GZIPEncodingInterceptor.class );
        return target.proxy( ManagementApi.class );
    }
}
