package com.enonic.cloud.operator.helpers;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main
{
    public static void main( String... args )
    {
        Quarkus.run( args );
    }
}


