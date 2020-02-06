package com.enonic.ec.kubernetes.operator.helm;

import java.io.File;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.collect.ImmutableMap;

public class HelmDependenciesProducer
{
    @ConfigProperty(name = "operator.helm.charts.path")
    String helmChartsPath;

    @Produces
    @Singleton
    @Named("local")
    public ChartRepository local()
    {
        return new LocalRepository( new File( helmChartsPath ) );
    }

    @Produces
    @Singleton
    @Named("baseValues")
    public Map<String, Object> baseValues()
    {
        return ImmutableMap.copyOf( new BaseValues() );
    }
}
