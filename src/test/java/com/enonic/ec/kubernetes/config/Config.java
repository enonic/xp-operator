package com.enonic.ec.kubernetes.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class Config
    implements ConfigSource
{
    private Map<String, String> map;

    public Config()
    {
        map = new HashMap<>();
        try (InputStream input = new FileInputStream( "./src/main/resources/application.properties" ))
        {
            Properties prop = new Properties();
            prop.load( input );
            prop.forEach( ( k, v ) -> map.put( (String) k, (String) v ) );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( ex );
        }
    }

    @Override
    public Map<String, String> getProperties()
    {
        return map;
    }

    @Override
    public String getValue( final String s )
    {
        return map.get( s );
    }

    @Override
    public String getName()
    {
        return "default";
    }
}
