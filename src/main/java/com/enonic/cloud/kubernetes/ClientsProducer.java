package com.enonic.cloud.kubernetes;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7config.Xp7ConfigClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7vhost.Xp7VHostClient;

public class ClientsProducer
{
    @Singleton
    @Produces
    Clients crdClient()
    {
        KubernetesClient client = new DefaultKubernetesClient().inAnyNamespace();
        return ClientsImpl.of( client, new Xp7AppClient( client ), new Xp7ConfigClient( client ), new Xp7DeploymentClient( client ),
                               new Xp7VHostClient( client ) );
    }
}
