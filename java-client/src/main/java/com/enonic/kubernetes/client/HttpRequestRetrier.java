package com.enonic.kubernetes.client;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.internal.ExponentialBackoffIntervalCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
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
        this.intervalCalculator = new ExponentialBackoffIntervalCalculator( builder.retryInterval, 5 );
    }

    public HttpResponse<InputStream> execute( final HttpRequest request )
    {
        return doExecuteWithRetry( request );
    }

    private HttpResponse<InputStream> doExecuteWithRetry( final HttpRequest request )
    {
        int currentTry = 1;

        while ( true )
        {
            try
            {
                HttpResponse<InputStream> response = doExecute( request );

                if ( conditionsToRetry.stream().anyMatch( f -> f.apply( response ) ) )
                {
                    if ( !sleepIfPossible( currentTry++ ) )
                    {
                        throw new RuntimeException( String.format( "HTTP operation on url: '%s' failed", request.uri() ) );
                    }
                }

            }
            catch ( IOException ex )
            {
                if ( !sleepIfPossible( currentTry++ ) )
                {
                    throw new UncheckedIOException( ex );
                }
            }
        }
    }

    private boolean sleepIfPossible( final int currentTry )
    {
        if ( currentTry <= retries )
        {
            try
            {
                Thread.sleep( intervalCalculator.getInterval( currentTry ) );
                return true;
            }
            catch ( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            return false;
        }
    }

    private HttpResponse<InputStream> doExecute( final HttpRequest request )
        throws IOException
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
        catch ( InterruptedException e )
        {
            throw new RuntimeException( String.format( "HTTP operation on url: '%s' failed", request.uri() ), e );
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

        private int retryInterval;

        public Builder client( final HttpClient client )
        {
            this.client = client;
            return this;
        }

        public Builder conditionsToRetry( final Collection<Function<HttpResponse<InputStream>, Boolean>> conditionsToRetry )
        {
            this.conditionsToRetry = conditionsToRetry;
            return this;

        }

        public Builder retries( final int retries )
        {
            this.retries = retries;
            return this;

        }

        public Builder retryInterval( final int retryInterval )
        {
            this.retryInterval = retryInterval;
            return this;

        }

        public HttpRequestRetrier build()
        {
            return new HttpRequestRetrier( this );
        }
    }
}
