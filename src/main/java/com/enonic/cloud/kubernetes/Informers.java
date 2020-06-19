package com.enonic.cloud.kubernetes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;


@Value.Immutable
@Params
public abstract class Informers
{
    protected abstract Clients clients();

    public abstract SharedInformerFactory informerFactory();

    public abstract SharedIndexInformer<ConfigMap> configMapInformer();

    public abstract SharedIndexInformer<Ingress> ingressInformer();

    public abstract SharedIndexInformer<Namespace> namespaceInformer();

    public abstract SharedIndexInformer<Pod> podInformer();

    public abstract SharedIndexInformer<Xp7App> xp7AppInformer();

    public abstract SharedIndexInformer<Xp7Config> xp7ConfigInformer();

    public abstract SharedIndexInformer<Xp7Deployment> xp7DeploymentInformer();

    public abstract SharedIndexInformer<Xp7VHost> xp7VHostInformer();

}
