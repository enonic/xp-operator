package com.enonic.kubernetes.helm.values;

import javax.inject.Named;
import javax.inject.Singleton;

import io.micronaut.context.annotation.Factory;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Factory
public class BaseValuesProducer
{
    @Singleton
    public BaseValues createBaseValues( @Named("clusterId") String clusterId )
    {
        singletonAssert(this, "createBaseValues");
        return new BaseValues( clusterId );
    }
}
