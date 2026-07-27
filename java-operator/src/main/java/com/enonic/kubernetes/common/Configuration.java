package com.enonic.kubernetes.common;

import java.util.HashMap;
import java.util.Map;

import io.micronaut.context.env.Environment;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "WeakerAccess", "unused"})
public final class Configuration
{
    private static Environment _env;

    public static void init( Environment environment )
    {
        _env = environment;
    }

    public static Environment globalConfig()
    {
        return _env;
    }

    public static Map<String, String> cfgStrChild( String parent )
    {
        Map<String, String> res = new HashMap<>();
        globalConfig().getProperties( parent ).forEach( ( key, value ) -> res.put( key, String.valueOf( value ) ) );
        return res;
    }

    public static boolean cfgHasKey( String key )
    {
        return globalConfig().getProperty( key, String.class ).isPresent();
    }

    public static String cfgStr( String key )
    {
        return globalConfig().getProperty( key, String.class ).get();
    }

    public static String cfgStrFmt( String key, Object... args )
    {
        return String.format( globalConfig().getProperty( key, String.class ).get(), args );
    }

    public static int cfgInt( String key )
    {
        return globalConfig().getProperty( key, Integer.class ).get();
    }

    public static long cfgLong( String key )
    {
        return globalConfig().getProperty( key, Long.class ).get();
    }

    public static boolean cfgBool( String key )
    {
        return globalConfig().getProperty( key, Boolean.class ).get();
    }

    public static float cfgFloat( String key )
    {
        return globalConfig().getProperty( key, Float.class ).get();
    }

    public static void cfgIfBool( String key, Runnable func )
    {
        if ( cfgBool( key ) )
        {
            func.run();
        }
    }
}
