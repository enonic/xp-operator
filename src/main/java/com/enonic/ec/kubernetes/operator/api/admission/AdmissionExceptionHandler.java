package com.enonic.ec.kubernetes.operator.api.admission;

import java.io.IOException;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import com.enonic.ec.kubernetes.crd.BuilderException;

public class AdmissionExceptionHandler
{
    public static String extractJacksonMessage( IOException ex )
    {
        if ( ex instanceof InvalidDefinitionException && ex.getCause() != null && ex.getCause() instanceof BuilderException )
        {
            return ex.getCause().getCause().getMessage();
        }
        if ( ex instanceof InvalidDefinitionException && ex.getCause() != null && ex.getCause() instanceof IllegalStateException )
        {
            return ex.getCause().getMessage();
        }
        if ( ex instanceof UnrecognizedPropertyException )
        {
            return "Field unrecognized: " + ( (UnrecognizedPropertyException) ex ).getPropertyName();
        }

        return ex.getMessage().replace( "\n", " " );
    }
}