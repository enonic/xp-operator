package com.enonic.kubernetes.helm.charts;

import java.io.File;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class RepositoriesProducer
{
    @ConfigProperty(name = "operator.charts.path")
    String helmChartsPath;

    @Produces
    @Singleton
    @Named("local")
    public ChartRepository createLocalRepository()
    {
        singletonAssert(this, "createLocalRepository");
        return new LocalRepository( new File( helmChartsPath ) );
    }
}
