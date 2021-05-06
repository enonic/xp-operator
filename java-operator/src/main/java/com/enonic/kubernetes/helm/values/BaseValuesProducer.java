package com.enonic.kubernetes.helm.values;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

public class BaseValuesProducer
{
    @Produces
    @Singleton
    public BaseValues producerBaseValues( @Named("clusterId") String clusterId )
    {
        return new BaseValues( clusterId );
    }
}
