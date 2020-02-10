package com.enonic.ec.kubernetes.operator.api;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseApi<T>
{
    @Inject
    public ObjectMapper mapper;

    @SuppressWarnings("unchecked")
    protected T getResult( String request, Class<T> k )
        throws IOException
    {
        Map<String, Object> admission = mapper.readValue( request, Map.class );
        // We have to extract the uid this way if there is a validation error during
        // mapping of the actual objects.
        String uid = (String) ( (Map) admission.get( "request" ) ).get( "uid" );

        // Hack until this is fixed: https://github.com/fabric8io/kubernetes-client/issues/1898
        ((Map<String, Object>)admission.get( "request" )).remove( "options" );
        request = mapper.writeValueAsString( admission );

        try
        {
            return process( uid, mapper.readValue( request, k ) );
        }
        catch ( Exception e )
        {
            return failure( uid, ApiExceptionHandler.extractJacksonMessage( e ) );
        }
    }

    protected abstract T process( final String uid, T obj );

    protected abstract T failure( final String uid, String message );
}
