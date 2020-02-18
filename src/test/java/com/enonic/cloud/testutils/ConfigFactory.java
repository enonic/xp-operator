package com.enonic.cloud.testutils;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class ConfigFactory
    extends SmallRyeConfigFactory
{
    @Override
    public SmallRyeConfig getConfigFor( final SmallRyeConfigProviderResolver configProviderResolver, final ClassLoader classLoader )
    {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDiscoveredSources();
        return builder.build();
    }
}
