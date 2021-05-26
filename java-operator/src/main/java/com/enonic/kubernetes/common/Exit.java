package com.enonic.kubernetes.common;

import io.quarkus.runtime.Quarkus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exit
{
    private static final Logger log = LoggerFactory.getLogger( Exit.class );

    public enum Code
    {
        INFORMER_FAILED( 701 ),
        SINGLETON_FAILED(702),
        STARTUP_FAILED( 703 );

        private final int value;

        Code( int value )
        {
            this.value = value;
        }
    }

    public static void exit( Code code, Throwable error )
    {
        exit( code, error.getMessage(), error );
    }

    public static void exit( Code code, String message )
    {
        exit( code, message, null );
    }

    public static void exit( Code code, String message, Throwable error )
    {
        log.error( "FATAL: " + message, error );
        Quarkus.asyncExit( code.value );
    }
}
