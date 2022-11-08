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

    private final int attempts;


    public HttpRequestRetrier( final Builder builder )
    {
        this.client = builder.client;
        this.attempts = builder.attempts;
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
        for ( int currentTry = 1; currentTry <= attempts; currentTry++ )
        {
            try
            {
                HttpResponse<InputStream> response = doExecute( request );

                if ( conditionsToRetry.stream().anyMatch( f -> f.apply( response ) ) && currentTry != attempts )
                {
                    Thread.sleep( intervalCalculator.getInterval( currentTry ) );
                }

            }
            catch ( IOException ex )
            {
                if ( currentTry == attempts )
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

        private int attempts;

        private Duration retryInterval;

        public Builder client( final HttpClient client )
        {
            this.client = client;
            return this;
        }

        public Builder conditionsToRetry( final Function<HttpResponse<InputStream>, Boolean>... conditionsToRetry )
        {
            this.conditionsToRetry = List.of(conditionsToRetry);
            return this;

        }

        public Builder attempts( final int retries )
        {
            this.attempts = retries;
            return this;

        }

        public Builder retryInterval( final Duration retryInterval )
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
