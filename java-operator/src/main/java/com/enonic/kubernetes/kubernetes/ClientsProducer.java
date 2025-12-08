package com.enonic.kubernetes.kubernetes;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;

import com.enonic.kubernetes.client.DefaultEnonicKubernetesClient;
import com.enonic.kubernetes.client.EnonicKubernetesClient;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.profile.UnlessBuildProfile;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class ClientsProducer
{
    @Singleton
    @Produces
    @IfBuildProfile("prod")
    Clients createClients()
    {
        singletonAssert(this, "createClients");

        final NamespacedKubernetesClient defaultKubernetesClient = new DefaultKubernetesClient().inAnyNamespace();
        final EnonicKubernetesClient client = new DefaultEnonicKubernetesClient(defaultKubernetesClient);

        return ClientsImpl.of(
                client.k8s(),
                client.enonic(),
                client.enonic().v1().crds().xp7apps(),
                client.enonic().v1().crds().xp7configs(),
                client.enonic().v1().crds().xp7deployments() );
    }
}
