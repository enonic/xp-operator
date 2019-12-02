package com.enonic.ec.kubernetes.common;

import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class Configuration
{
    private static Config _cfg;

    private static Config config()
    {
        if ( _cfg == null )
        {
            _cfg = ConfigProvider.getConfig();
        }
        return _cfg;
    }

    protected static String cfgStr( String key )
    {
        return config().getOptionalValue( key, String.class ).get();
    }

    @SuppressWarnings("SameParameterValue")
    protected static String cfgStrFmt( String key, Object... args )
    {
        return String.format( config().getOptionalValue( key, String.class ).get(), args );
    }

    protected static int cfgInt( String key )
    {
        return config().getOptionalValue( key, Integer.class ).get();
    }

    protected static long cfgLong( String key )
    {
        return config().getOptionalValue( key, Long.class ).get();
    }

    protected static List<String> cfgList( String key )
    {
        return config().getOptionalValue( key, List.class ).get();
    }

    protected static boolean cfgBool( String key )
    {
        return config().getOptionalValue( key, Boolean.class ).get();
    }

    protected static void cfgIfBool( String key, Runnable func )
    {
        if ( cfgBool( key ) )
        {
            func.run();
        }
    }
}
