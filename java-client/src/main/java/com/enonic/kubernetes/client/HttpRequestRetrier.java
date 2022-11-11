package com.enonic.kubernetes.client;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.internal.ExponentialBackoffIntervalCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class HttpRequestRetrier
{
    private final HttpClient client;

    private final Collection<Function<HttpResponse<InputStream>, Boolean>> conditionsToRetry;

    private final ExponentialBackoffIntervalCalculator intervalCalculator;

    private final int retries;


    public HttpRequestRetrier( final Builder builder )
    {
        this.client = builder.client;
        this.retries = builder.retries;
        this.conditionsToRetry = builder.conditionsToRetry;
        this.intervalCalculator = new ExponentialBackoffIntervalCalculator( (int) builder.retryInterval.toMillis(), 5 );
    }

    public HttpResponse<InputStream> execute( final HttpRequest request )
        throws InterruptedException, IOException
    {
        return doExecuteWithRetry( request );
    }

    private HttpResponse<InputStream> doExecuteWithRetry( final HttpRequest request )
        throws InterruptedException, IOException
    {
        for ( int currentTry = 0; currentTry <= retries; currentTry++ )
        {
            try
            {
                HttpResponse<InputStream> response = doExecute( request );

                if ( conditionsToRetry.stream().anyMatch( f -> f.apply( response ) ) )
                {
                    if ( currentTry == retries )
                    {
                        throw new RuntimeException(
                            String.format( "HTTP operation on url: '%s' failed with '%s' status", request.uri(), response.code() ) );
                    }

                    Thread.sleep( intervalCalculator.getInterval( currentTry ) );
                }
                else
                {
                    return response;
                }

            }
            catch ( IOException ex )
            {
                if ( currentTry == retries )
                {
                    throw ex;
                }

                Thread.sleep( intervalCalculator.getInterval( currentTry ) );
            }
        }

        throw new RuntimeException( String.format( "HTTP operation on url: '%s' failed", request.uri() ) );
    }

    private HttpResponse<InputStream> doExecute( final HttpRequest request )
        throws IOException, InterruptedException
    {
        try
        {
            return client.sendAsync( request, InputStream.class ).get();
        }
        catch ( ExecutionException e )
        {
            final Throwable cause = e.getCause();

            if ( cause instanceof IOException )
            {
                throw (IOException) cause;
            }
            else
            {
                throw new RuntimeException( String.format( "HTTP operation on url: '%s' failed", request.uri() ), cause );
            }
        }
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static class Builder
    {
        private HttpClient client;

        private Collection<Function<HttpResponse<InputStream>, Boolean>> conditionsToRetry;

        private int retries;

        private Duration retryInterval;

        public Builder client( final HttpClient client )
        {
            this.client = client;
            return this;
        }

        public Builder conditionsToRetry( final Function<HttpResponse<InputStream>, Boolean>... conditionsToRetry )
        {
            this.conditionsToRetry = List.of( conditionsToRetry );
            return this;

        }

        public Builder retries( final int retries )
        {
            this.retries = retries;
            return this;

        }

        public Builder retryInterval( final Duration retryInterval )
        {
            this.retryInterval = retryInterval;
            return this;

        }

        private void validate()
        {
            if ( retries < 0 )
            {
                throw new IllegalArgumentException( "retries must be positive or 0" );
            }
        }

        public HttpRequestRetrier build()
        {
            validate();
            return new HttpRequestRetrier( this );
        }
    }
}
