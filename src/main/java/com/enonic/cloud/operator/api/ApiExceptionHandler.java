package com.enonic.cloud.operator.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;


class ApiExceptionHandler
{
    private final static Logger log = LoggerFactory.getLogger( ApiExceptionHandler.class );

    public static String extractSerializationExceptionMessage( Throwable ex )
    {
        if ( ex instanceof IllegalStateException )
        {
            return ex.getMessage();
        }
        if ( ex instanceof ValueInstantiationException )
        {
            return extractSerializationExceptionMessage( ex.getCause() );
        }
        if ( ex instanceof UnrecognizedPropertyException )
        {
            return "Field unrecognized: " + ( (UnrecognizedPropertyException) ex ).getPropertyName();
        }
        if ( ex instanceof InvalidFormatException )
        {
            return ( (InvalidFormatException) ex ).getOriginalMessage();
        }

        log.error( "Unknown exception in admission api", ex );

        return ex.getMessage().replace( "\n", " " );
    }
}
