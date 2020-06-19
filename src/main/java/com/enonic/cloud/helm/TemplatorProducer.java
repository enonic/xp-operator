package com.enonic.cloud.helm;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import com.enonic.cloud.helm.charts.ChartRepository;
import com.enonic.cloud.helm.functions.Templator;

public class TemplatorProducer
{
    @Produces
    @Singleton
    @Named("v1alpha2/xp7vhost")
    public Templator producerTemplatorV1Alpha2Xp7VHost( Helm helm, @Named("local") ChartRepository chartRepository )
    {
        return ( values -> helm.templateObjects( chartRepository.get( "v1alpha2/xp7vhost" ), values ) );
    }

    @Produces
    @Singleton
    @Named("v1alpha2/xp7deployment")
    public Templator producerTemplatorV1Alpha2Xp7Deployment( Helm helm, @Named("local") ChartRepository chartRepository )
    {
        return ( values -> helm.templateObjects( chartRepository.get( "v1alpha2/xp7deployment" ), values ) );
    }
}
