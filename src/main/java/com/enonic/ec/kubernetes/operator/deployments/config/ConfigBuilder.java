package com.enonic.ec.kubernetes.operator.deployments.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class ConfigBuilder
    extends Configuration
{
    public abstract Map<String, String> baseConfig();

    protected Map<String, String> overrideWithValues( Map<String, String> newConfig )
    {
        Map<String, String> res = new HashMap<>( baseConfig() );
        newConfig.entrySet().forEach( e -> res.put( e.getKey(), e.getValue() ) );
        return res;
    }

    protected void setIfNotSet( Map<String, String> config, String configFileKey, Consumer<BiFunction<String, Object, Boolean>> applyFunc )
    {
        Properties p = getAsProperties( config, configFileKey ).orElse( new Properties() );
        applyFunc.accept( ( key, value ) -> {
            Object v = p.get( key );
            if ( v == null )
            {
                p.put( key, value );
                return true;
            }
            return false;
        } );
        config.put( configFileKey, getPropertyAsString( p ) );
    }

    private static Optional<Properties> getAsProperties( Map<String, String> config, String key )
    {
        Optional<String> file = Optional.ofNullable( config.get( key ) );
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

    private static String getPropertyAsString( Properties prop )
    {
        StringBuilder sb = new StringBuilder();
        prop.forEach( ( k, v ) -> sb.append( k.toString() ).append( " = " ).append( v.toString() ).append( "\n" ) );
        return sb.toString();
    }
}
