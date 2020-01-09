package com.enonic.ec.kubernetes.operator.common;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class Configuration
{
    private static Config _cfg;

    protected static Config globalConfig()
    {
        if ( _cfg == null )
        {
            _cfg = ConfigProvider.getConfig();
        }
        return _cfg;
    }

    public static String cfgStr( String key )
    {
        return globalConfig().getOptionalValue( key, String.class ).get();
    }

    @SuppressWarnings("SameParameterValue")
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

    public static void cfgIfBool( String key, Runnable func )
    {
        if ( cfgBool( key ) )
        {
            func.run();
        }
    }
}
