package com.enonic.kubernetes.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import com.enonic.kubernetes.crd.v1.Xp7App;
import com.enonic.kubernetes.crd.v1.Xp7Config;
import com.enonic.kubernetes.crd.v1.Xp7Deployment;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.configuration.ProfileManager;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class ClientsProducer
{
    @Singleton
    @Produces
    @IfBuildProfile("prod")
    Clients createClients()
    {
        singletonAssert(this, "createClients");

        ProfileManager.getActiveProfile();

        final NamespacedKubernetesClient k8s = new DefaultKubernetesClient().inAnyNamespace();

        return ClientsImpl.of(
                k8s,
                k8s.resources( Xp7App.class ),
                k8s.resources( Xp7Config.class ),
                k8s.resources( Xp7Deployment.class ) );
    }
}
