package com.enonic.cloud.apis.xp;

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

import com.enonic.cloud.apis.xp.service.AdminApi;
import com.enonic.cloud.apis.xp.service.CustomRestHeaderFilter;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Value.Immutable
public abstract class XpClientCreator
{
    @Value.Default
    public String serviceName()
    {
        return cfgStr( "operator.helm.charts.Values.allNodesKey" );
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
//        return "localhost";
        return String.format( "%s.%s.svc.cluster.local", serviceName(), namespace() );
    }

    @Value.Default
    protected String adminPort()
    {
//        return "8081";
        return "8080";
    }

    @Value.Derived
    protected BasicAuthentication basicAuthentication()
    {
        return new BasicAuthentication( username(), password() );
    }

    @Value.Derived
    protected ResteasyClient client()
    {
        return new ResteasyClientBuilderImpl().httpEngine( new ApacheHttpClient43Engine() ).build();
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
    public AdminApi adminApi()
    {
        return createTarget( UriBuilder.fromPath( "http://" + deploymentHost() + ":" + adminPort() ) ).proxy( AdminApi.class );
    }
}
