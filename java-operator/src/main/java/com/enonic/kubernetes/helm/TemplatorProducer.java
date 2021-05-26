package com.enonic.kubernetes.helm;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import com.enonic.kubernetes.helm.charts.ChartRepository;
import com.enonic.kubernetes.helm.functions.Templator;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class TemplatorProducer
{
    @Produces
    @Singleton
    @Named("v1alpha2/xp7deployment")
    public Templator createTemplator( Helm helm, @Named("local") ChartRepository chartRepository )
    {
        singletonAssert(this, "createTemplator");
        return ( values -> helm.templateObjects( chartRepository.get( "v1alpha2/xp7deployment" ), values ) );
    }
}
