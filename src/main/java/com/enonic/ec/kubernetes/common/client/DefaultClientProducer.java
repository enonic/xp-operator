package com.enonic.ec.kubernetes.common.client;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class DefaultClientProducer
{

    @Produces
    @Singleton
    @Named("default")
    KubernetesClient produceDefaultClient()
    {
        return new DefaultKubernetesClient().inAnyNamespace();
    }

}
