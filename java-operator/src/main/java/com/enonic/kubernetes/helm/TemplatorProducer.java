package com.enonic.kubernetes.helm;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import com.enonic.kubernetes.helm.charts.ChartRepository;
import com.enonic.kubernetes.helm.functions.Templator;

public class TemplatorProducer
{
    @Produces
    @Singleton
    @Named("v1alpha2/xp7deployment")
    public Templator producerTemplatorV1Alpha2Xp7Deployment( Helm helm, @Named("local") ChartRepository chartRepository )
    {
        return ( values -> helm.templateObjects( chartRepository.get( "v1alpha2/xp7deployment" ), values ) );
    }
}
