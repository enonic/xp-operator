package com.enonic.kubernetes.client.v1;

import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class CrdApiImpl implements CrdApi {

    private final KubernetesClient k8sClient;

    public CrdApiImpl(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    @Override
    public MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> xp7apps() {
        return k8sClient.resources(Xp7App.class, Xp7App.Xp7AppList.class);
    }

    @Override
    public MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>> xp7configs() {
        return k8sClient.resources(Xp7Config.class, Xp7Config.Xp7ConfigList.class);
    }

    @Override
    public MixedOperation<Xp7Deployment, Xp7Deployment.Xp7DeploymentList, Resource<Xp7Deployment>> xp7deployments() {
        return k8sClient.resources(Xp7Deployment.class, Xp7Deployment.Xp7DeploymentList.class);
    }
}
