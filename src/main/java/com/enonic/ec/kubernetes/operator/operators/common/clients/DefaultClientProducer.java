package com.enonic.ec.kubernetes.operator.operators.common.clients;

import javax.inject.Singleton;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Singleton
public class DefaultClientProducer
{
    private final KubernetesClient kubernetesClient;

    public DefaultClientProducer()
    {
        kubernetesClient = new DefaultKubernetesClient().inAnyNamespace();
    }

    public KubernetesClient client()
    {
        return kubernetesClient;
    }
}
