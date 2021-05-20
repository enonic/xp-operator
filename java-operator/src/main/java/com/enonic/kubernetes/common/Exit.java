package com.enonic.kubernetes.common;

import io.quarkus.runtime.Quarkus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exit
{
    private static final Logger log = LoggerFactory.getLogger( Exit.class );

    public enum Code
    {
        INFORMER_FAILED( 156 );

        private final int value;

        Code( int value )
        {
            this.value = value;
        }
    }

    public static void exit( Code code, String message )
    {
        log.error( "FATAL: " + message );
        Quarkus.asyncExit( code.value );
    }
}
