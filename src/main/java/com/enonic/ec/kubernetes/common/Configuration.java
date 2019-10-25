package com.enonic.ec.kubernetes.common;

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

    protected String cfgStr( String key )
    {
        return config().getOptionalValue( key, String.class ).get();
    }

    protected int cfgInt( String key )
    {
        return config().getOptionalValue( key, Integer.class ).get();
    }

    protected long cfgLong( String key )
    {
        return config().getOptionalValue( key, Long.class ).get();
    }
}
