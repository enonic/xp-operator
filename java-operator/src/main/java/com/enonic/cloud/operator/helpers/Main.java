package com.enonic.cloud.operator.helpers;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Run this method in you IDE to start the operator
 */
@QuarkusMain
public class Main
{
    public static void main( String... args )
    {
        Quarkus.run( args );
    }
}


