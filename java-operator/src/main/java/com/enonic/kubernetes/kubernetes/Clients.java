package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.client.CustomClient;
import org.immutables.value.Value;


import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;


import com.enonic.kubernetes.common.annotations.Params;
import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;


@Value.Immutable
@Params
public interface Clients
{
    KubernetesClient k8s();

    CustomClient enonic();

    MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> xp7Apps();

    MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>> xp7Configs();

    MixedOperation<Xp7Deployment, Xp7Deployment.Xp7DeploymentList, Resource<Xp7Deployment>> xp7Deployments();

    MixedOperation<Domain, Domain.DomainList, Resource<Domain>> domain();
}
