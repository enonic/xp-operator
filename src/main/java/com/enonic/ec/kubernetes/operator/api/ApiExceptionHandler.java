package com.enonic.ec.kubernetes.operator.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

public class ApiExceptionHandler
{
    private final static Logger log = LoggerFactory.getLogger( ApiExceptionHandler.class );

    public static String extractJacksonMessage( Exception ex )
    {
        if ( ex instanceof InvalidDefinitionException && ex.getCause() != null && ex.getCause() instanceof BuilderException )
        {
            return ex.getCause().getCause().getMessage();
        }
        if ( ex instanceof InvalidDefinitionException && ex.getCause() != null && ex.getCause() instanceof IllegalStateException )
        {
            return ex.getCause().getMessage();
        }
        if ( ex instanceof IllegalStateException )
        {
            return ex.getMessage();
        }
        if ( ex instanceof UnrecognizedPropertyException )
        {
            return "Field unrecognized: " + ( (UnrecognizedPropertyException) ex ).getPropertyName();
        }
        if ( ex instanceof JsonMappingException ) {
            return ( (JsonMappingException) ex ).getOriginalMessage();
        }

        log.error( "Unknown exception in admission api", ex );

        return ex.getMessage().replace( "\n", " " );
    }
}
