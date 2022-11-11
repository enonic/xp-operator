package com.enonic.kubernetes.client.v1.api.operator.v1;

import com.enonic.kubernetes.client.HttpRequestRetrier;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class HttpRequestRetrierTest
{
    @Test
    public void testRuntimeException()
        throws Exception
    {
        final HttpClient httpClient = mock( HttpClient.class );
        HttpRequest request = mock( HttpRequest.class );

        final HttpRequestRetrier retrier = HttpRequestRetrier.create()
            .retries( 10 )
            .retryInterval( Duration.ofMillis( 100 ) )
            .conditionsToRetry( ( response ) -> response.code() >= 500 )
            .client( httpClient )
            .build();

        final String testErrorMessage = "test error message";

        when( httpClient.sendAsync( request, InputStream.class ) ).thenThrow( new RuntimeException( testErrorMessage ) );

        final Exception ex = Assertions.assertThrows( RuntimeException.class, () -> retrier.execute( request ) );
        Assertions.assertEquals( testErrorMessage, ex.getMessage() );

        verify( httpClient, times( 1 ) ).sendAsync( request, InputStream.class );
    }

    @Test
    public void testIOExecutionException()
        throws Exception
    {
        final HttpClient httpClient = mock( HttpClient.class );
        final HttpRequest request = mock( HttpRequest.class );
        final CompletableFuture<HttpResponse<InputStream>> future = mock( CompletableFuture.class );

        final HttpRequestRetrier retrier = HttpRequestRetrier.create()
            .retries( 3 )
            .retryInterval( Duration.ofMillis( 100 ) )
            .conditionsToRetry( ( response ) -> response.code() >= 500 )
            .client( httpClient )
            .build();

        final String causeErrorMessage = "cause error message";

        when( httpClient.sendAsync( request, InputStream.class ) ).thenReturn( future );
        when( future.get() ).thenThrow( new ExecutionException( "test error message", new IOException( causeErrorMessage ) ) );

        final Exception ex = Assertions.assertThrows( IOException.class, () -> retrier.execute( request ) );
        Assertions.assertEquals( causeErrorMessage, ex.getMessage() );

        verify( httpClient, times( 4 ) ).sendAsync( request, InputStream.class );

    }

    @Test
    public void testAnyOtherExecutionException()
        throws Exception
    {
        final HttpClient httpClient = mock( HttpClient.class );
        final HttpRequest request = mock( HttpRequest.class );
        final CompletableFuture<HttpResponse<InputStream>> future = mock( CompletableFuture.class );

        final HttpRequestRetrier retrier = HttpRequestRetrier.create()
            .retries( 10 )
            .retryInterval( Duration.ofMillis( 500 ) )
            .conditionsToRetry( ( response ) -> response.code() >= 500 )
            .client( httpClient )
            .build();

        final String causeErrorMessage = "cause error message";

        when( httpClient.sendAsync( request, InputStream.class ) ).thenReturn( future );
        when( future.get() ).thenThrow( new ExecutionException( "test error message", new Exception( causeErrorMessage ) ) );

        final Exception ex = Assertions.assertThrows( RuntimeException.class, () -> retrier.execute( request ) );
        Assertions.assertEquals( causeErrorMessage, ex.getCause().getMessage() );

        verify( httpClient, times( 1 ) ).sendAsync( request, InputStream.class );

    }

    @Test
    public void test500Exception()
        throws Exception
    {
        final HttpClient httpClient = mock( HttpClient.class );
        final HttpRequest request = mock( HttpRequest.class );
        final HttpResponse<InputStream> response = mock( HttpResponse.class );
        final CompletableFuture<HttpResponse<InputStream>> future = mock( CompletableFuture.class );

        when( response.code() ).thenReturn( 500 );
        when( request.uri() ).thenReturn( URI.create( "http://localhost" ) );

        final HttpRequestRetrier retrier = HttpRequestRetrier.create()
            .retries( 4 )
            .retryInterval( Duration.ofMillis( 100 ) )
            .conditionsToRetry( ( r ) -> r.code() >= 500 )
            .client( httpClient )
            .build();

        when( httpClient.sendAsync( request, InputStream.class ) ).thenReturn( future );
        when( future.get() ).thenReturn( response );

        final Exception ex = Assertions.assertThrows( RuntimeException.class, () -> retrier.execute( request ) );
        Assertions.assertEquals( "HTTP operation on url: \'http://localhost\' failed with '500' status", ex.getMessage() );

        verify( httpClient, times( 5 ) ).sendAsync( request, InputStream.class );
    }

    @Test
    public void testInterruptedException()
        throws Exception
    {
        final HttpClient httpClient = mock( HttpClient.class );
        HttpRequest request = mock( HttpRequest.class );
        final CompletableFuture<HttpResponse<InputStream>> future = mock( CompletableFuture.class );

        final HttpRequestRetrier retrier = HttpRequestRetrier.create()
            .retries( 10 )
            .retryInterval( Duration.ofMillis( 100 ) )
            .conditionsToRetry( response -> response.code() >= 500 )
            .client( httpClient )
            .build();

        final String testErrorMessage = "test error message";

        when( httpClient.sendAsync( request, InputStream.class ) ).thenReturn( future );
        when( future.get() ).thenThrow( new InterruptedException( testErrorMessage ) );

        final Exception ex = Assertions.assertThrows( InterruptedException.class, () -> retrier.execute( request ) );
        Assertions.assertEquals( testErrorMessage, ex.getMessage() );

        verify( httpClient, times( 1 ) ).sendAsync( request, InputStream.class );
    }
}
