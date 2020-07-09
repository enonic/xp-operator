package com.enonic.cloud.kubernetes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.domain.DomainClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7config.Xp7ConfigClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentClient;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7vhost.Xp7VHostClient;


@Value.Immutable
@Params
public abstract class Clients
{
    public abstract KubernetesClient k8s();

    public abstract Xp7AppClient xp7Apps();

    public abstract Xp7ConfigClient xp7Configs();

    public abstract Xp7DeploymentClient xp7Deployments();

    public abstract Xp7VHostClient xp7VHosts();

    public abstract DomainClient domain();
}
