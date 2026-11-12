package com.enonic.kubernetes.common;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "WeakerAccess", "unused"})
public final class Configuration
{
    private static Config _cfg;

    public static Config globalConfig()
    {
        if ( _cfg == null )
        {
            _cfg = ConfigProvider.getConfig();
        }
        return _cfg;
    }

    public static Map<String, String> cfgStrChild( String parent )
    {
        Map<String, String> res = new HashMap<>();
        globalConfig().getPropertyNames().forEach( key -> {
            String pKey = parent + ".";
            if ( key.startsWith( pKey ) )
            {
                res.put( key.replace( pKey, "" ), cfgStr( key ) );
            }
        } );
        return res;
    }

    public static boolean cfgHasKey( String key )
    {
        return globalConfig().getOptionalValue( key, String.class ).isPresent();
    }

    public static String cfgStr( String key )
    {
        return globalConfig().getOptionalValue( key, String.class ).get();
    }

    public static String cfgStrFmt( String key, Object... args )
    {
        return String.format( globalConfig().getOptionalValue( key, String.class ).get(), args );
    }

    public static int cfgInt( String key )
    {
        return globalConfig().getOptionalValue( key, Integer.class ).get();
    }

    public static long cfgLong( String key )
    {
        return globalConfig().getOptionalValue( key, Long.class ).get();
    }

    public static boolean cfgBool( String key )
    {
        return globalConfig().getOptionalValue( key, Boolean.class ).get();
    }

    public static float cfgFloat( String key )
    {
        return globalConfig().getOptionalValue( key, Float.class ).get();
    }

    public static void cfgIfBool( String key, Runnable func )
    {
        if ( cfgBool( key ) )
        {
            func.run();
        }
    }
}
