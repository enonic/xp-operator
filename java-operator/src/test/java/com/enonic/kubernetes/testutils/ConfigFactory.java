package com.enonic.kubernetes.testutils;

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
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();

        builder.addDiscoveredSources();
        builder.addDiscoveredConverters();
        builder.addDiscoveredValidator();
        builder.addDiscoveredInterceptors();

        return builder.build();
    }
}
