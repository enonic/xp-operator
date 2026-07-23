package com.enonic.kubernetes.common;

import javax.inject.Singleton;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.env.Environment;

// @Context forces eager (rather than lazy) instantiation as soon as the ApplicationContext
// starts, so Configuration's static cfgXxx(...) helpers are usable immediately, before any
// other bean is created.
@Context
@Singleton
public class ConfigurationInitializer
{
    public ConfigurationInitializer( Environment environment )
    {
        Configuration.init( environment );
    }
}
