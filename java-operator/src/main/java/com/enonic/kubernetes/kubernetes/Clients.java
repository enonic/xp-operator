package com.enonic.kubernetes.kubernetes;

import org.immutables.value.Value;


import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;


import com.enonic.kubernetes.common.annotations.Params;
import com.enonic.kubernetes.crd.v1.Xp7App;
import com.enonic.kubernetes.crd.v1.Xp7Config;
import com.enonic.kubernetes.crd.v1.Xp7Deployment;


@Value.Immutable
@Params
public interface Clients
{
    KubernetesClient k8s();

    MixedOperation<Xp7App, KubernetesResourceList<Xp7App>, Resource<Xp7App>> xp7Apps();

    MixedOperation<Xp7Config, KubernetesResourceList<Xp7Config>, Resource<Xp7Config>> xp7Configs();

    MixedOperation<Xp7Deployment, KubernetesResourceList<Xp7Deployment>, Resource<Xp7Deployment>> xp7Deployments();

}
