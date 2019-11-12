package com.enonic.ec.kubernetes.api;

import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

@Provider
public class ErrorHandler
    implements ExceptionMapper<Throwable>
{

    private final static Logger log = LoggerFactory.getLogger( ErrorHandler.class );

    @Override
    public Response toResponse( Throwable e )
    {
        Throwable err = e;

        if ( e instanceof InvalidDefinitionException && e.getCause() != null && e.getCause() instanceof IllegalStateException )
        {
            err = e.getCause();
        }

        int code = err instanceof IllegalStateException ? 400 : 500;
        String message = err.getMessage() != null ? err.getMessage() : "No message";
        String cause = err.getClass().getSimpleName();

        log.error( "Error during request", err );

        return Response.status( code ).entity( Map.of( "cause", cause, "message", message ) ).build();
    }
}
