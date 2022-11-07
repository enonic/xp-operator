package com.enonic.kubernetes.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.Serialization;
import okhttp3.HttpUrl;


import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public abstract class RawClient
{
    private final HttpClient client;

    private final String baseUrl;

    private final HttpRequestRetrier httpRequestRetrier;

    public RawClient( final HttpClient client, final Config config, String apiVersion )
    {
        this.client = client;
        int requestRetryBackoffLimit = config.getRequestRetryBackoffLimit();
        int requestRetryBackoffInterval = config.getRequestRetryBackoffInterval();
        this.baseUrl = String.format( "%s%s%s", config.getMasterUrl(), "apis/operator.enonic.cloud/", apiVersion );

        this.httpRequestRetrier = HttpRequestRetrier.create()
            .retries( requestRetryBackoffLimit )
            .conditionsToRetry( List.of( ( response ) -> response.code() >= 500 ) )
            .retryInterval( requestRetryBackoffInterval )
            .client( client )
            .build();
    }

    protected String baseUrl()
    {
        return baseUrl;
    }

    protected HttpUrl requestUrl( String... url )
    {
        StringBuilder sb = new StringBuilder( baseUrl() );
        for ( String s : url )
        {
            if ( !s.startsWith( "/" ) )
            {
                sb.append( "/" );
            }
            sb.append( s );
        }
        return Objects.requireNonNull( HttpUrl.parse( sb.toString() ) ).newBuilder().build();
    }

    protected <T> T request( HttpUrl httpUrl, Class<T> type )
    {
        try
        {
            return request( client.newHttpRequestBuilder().url( httpUrl.url() ).build(), type );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "RawClient request failed", e );
        }
    }

    private <T> T request( HttpRequest request, Class<T> type )
    {
        final HttpResponse<InputStream> response = retryWithExponentialBackoff( request );
        return Serialization.unmarshal( response.body(), type );
    }

    private HttpResponse<InputStream> retryWithExponentialBackoff( HttpRequest request )
    {
        return httpRequestRetrier.execute( request );
    }
}
