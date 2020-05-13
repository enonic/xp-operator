package com.enonic.cloud.operator.api;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseApi<T>
{
    @Inject
    public ObjectMapper mapper;

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    protected T getResult( String request, Class<T> k )
        throws IOException
    {
        Map<String, Object> r = mapper.readValue( request, Map.class );

        // Hack until this is fixed: https://github.com/fabric8io/kubernetes-client/issues/1898
        ( (Map<String, Object>) r.get( "request" ) ).remove( "options" );
        request = mapper.writeValueAsString( r );

        try
        {
            return process( mapper.readValue( request, k ) );
        }
        catch ( Exception e )
        {
            Map<String, Object> admission = mapper.readValue( request, Map.class );
            return failure( createOnFailure( admission ), ApiExceptionHandler.extractSerializationExceptionMessage( e ) );
        }
    }

    protected abstract T createOnFailure( Map<String, Object> request );

    protected abstract T process( T obj );

    protected abstract T failure( T obj, String message );
}
