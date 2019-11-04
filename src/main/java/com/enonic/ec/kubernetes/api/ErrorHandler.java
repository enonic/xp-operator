package com.enonic.ec.kubernetes.api;

import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

@Provider
public class ErrorHandler
    implements ExceptionMapper<Throwable>
{

    @Override
    public Response toResponse( Throwable e )
    {
        Throwable err = e;

        if ( e instanceof InvalidDefinitionException && e.getCause() != null && e.getCause() instanceof IllegalStateException )
        {
            err = e.getCause();
        }

        int code = err instanceof IllegalStateException ? 400 : 500;
        String message = err.getMessage();
        String cause = err.getClass().getSimpleName();

        return Response.status( code ).entity( Map.of( "cause", cause, "message", message ) ).build();
    }
}
