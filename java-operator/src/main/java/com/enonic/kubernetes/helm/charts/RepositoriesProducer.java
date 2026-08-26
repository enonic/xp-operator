package com.enonic.kubernetes.helm.charts;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Factory
public class RepositoriesProducer
{
    @Value("${operator.charts.path}")
    String helmChartsPath;

    @Singleton
    @Named("local")
    public ChartRepository createLocalRepository()
    {
        singletonAssert(this, "createLocalRepository");
        return new LocalRepository( new File( helmChartsPath ) );
    }
}
