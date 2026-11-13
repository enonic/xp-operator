package com.enonic.kubernetes.kubernetes;

import org.jspecify.annotations.NullMarked;



import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;


import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;


@NullMarked
public record Clients(
    KubernetesClient k8s,
    MixedOperation<Xp8Config,
    KubernetesResourceList<Xp8Config>,
    Resource<Xp8Config>> xp8Configs,
    MixedOperation<Xp8Deployment,
    KubernetesResourceList<Xp8Deployment>,
    Resource<Xp8Deployment>> xp8Deployments )
{
}
