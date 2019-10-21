package com.enonic.ec.kubernetes.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class ApiTest
{
    private String loadFile( String file )
    {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader( new InputStreamReader( this.getClass().getResourceAsStream( file ) ) ))
        {
            String line;
            while ( ( line = br.readLine() ) != null )
            {
                resultStringBuilder.append( line ).append( "\n" );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        return resultStringBuilder.toString();
    }
}
