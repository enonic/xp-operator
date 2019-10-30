package com.enonic.ec.kubernetes.deployment.spec;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;


public class SpecNodeConfig
    extends HashMap<String, String>
{
    public Optional<Properties> getAsProperties( String key )
    {
        Optional<String> file = Optional.ofNullable( get( key ) );
        if ( file.isEmpty() )
        {
            return Optional.empty();
        }

        Properties p = new Properties();
        try
        {
            p.load( new StringReader( file.get() ) );
            return Optional.of( p );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( ex );
        }
    }
}
