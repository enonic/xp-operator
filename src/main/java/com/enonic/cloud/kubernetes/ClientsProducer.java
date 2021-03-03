package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.cloud.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.cloud.kubernetes.client.v1alpha2.Domain;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;

public class ClientsProducer
{
    @Singleton
    @Produces
    Clients crdClient()
    {
        KubernetesClient client = new DefaultKubernetesClient().inAnyNamespace();
        return ClientsImpl.of( client, Xp7App.createCrdClient( client ), Xp7Config.createCrdClient( client ),
                               Xp7Deployment.createCrdClient( client ), Domain.createCrdClient( client ) );
    }
}
