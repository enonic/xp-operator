package com.enonic.kubernetes.helm.values;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class BaseValuesProducer
{
    @Produces
    @Singleton
    public BaseValues createBaseValues( @Named("clusterId") String clusterId )
    {
        singletonAssert(this, "createBaseValues");
        return new BaseValues( clusterId );
    }
}
