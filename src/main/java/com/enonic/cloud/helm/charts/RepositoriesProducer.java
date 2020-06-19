package com.enonic.cloud.helm.charts;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class RepositoriesProducer
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
}
