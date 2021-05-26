package com.enonic.kubernetes.helm.values;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

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
