package com.enonic.kubernetes.kubernetes;

import javax.inject.Singleton;

import com.enonic.kubernetes.crd.v1.Xp7Config;
import com.enonic.kubernetes.crd.v1.Xp7Deployment;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.micronaut.context.annotation.Factory;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Factory
public class ClientsProducer
{
    @Singleton
    Clients createClients()
    {
        singletonAssert(this, "createClients");

        final KubernetesClient k8s = new KubernetesClientBuilder().build();

        return ClientsImpl.of(
                k8s,
                k8s.resources( Xp7Config.class ),
                k8s.resources( Xp7Deployment.class ) );
    }
}
