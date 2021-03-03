package com.enonic.kubernetes.kubernetes;

import org.immutables.value.Value;


import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;


import com.enonic.kubernetes.common.annotations.Params;
import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;


@Value.Immutable
@Params
public abstract class Clients
{
    public abstract KubernetesClient k8s();

    public abstract MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> xp7Apps();

    public abstract MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>> xp7Configs();

    public abstract MixedOperation<Xp7Deployment, Xp7Deployment.Xp7DeploymentList, Resource<Xp7Deployment>> xp7Deployments();

    public abstract MixedOperation<Domain, Domain.DomainList, Resource<Domain>> domain();
}
