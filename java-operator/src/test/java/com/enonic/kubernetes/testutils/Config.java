package com.enonic.kubernetes.testutils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class Config
    implements ConfigSource
{
    private static final Map<String, String> MAP;

    static {
        MAP = new HashMap<>();
        try (InputStream input = new FileInputStream( "./src/test/resources/application.properties" ))
        {
            Properties prop = new Properties();
            prop.load( input );
            prop.forEach( ( k, v ) -> MAP.put( (String) k, (String) v ) );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( ex );
        }
    }

    public Config()
    {
        int i = 0;
    }

    @Override
    public Map<String, String> getProperties()
    {
        return MAP;
    }

    @Override
    public Set<String> getPropertyNames()
    {
        return MAP.keySet();
    }

    @Override
    public String getValue( final String s )
    {
        return MAP.get( s );
    }

    @Override
    public String getName()
    {
        return "default";
    }
}
