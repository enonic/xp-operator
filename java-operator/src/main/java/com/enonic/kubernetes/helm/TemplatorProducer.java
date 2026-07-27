package com.enonic.kubernetes.helm;

import javax.inject.Named;
import javax.inject.Singleton;

import io.micronaut.context.annotation.Factory;

import com.enonic.kubernetes.helm.charts.ChartRepository;
import com.enonic.kubernetes.helm.functions.Templator;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Factory
public class TemplatorProducer
{
    @Singleton
    @Named("v1/xp7deployment")
    public Templator createTemplator( Helm helm, @Named("local") ChartRepository chartRepository )
    {
        singletonAssert(this, "createTemplator");
        return ( values -> helm.templateObjects( chartRepository.get( "v1/xp7deployment" ), values ) );
    }
}
