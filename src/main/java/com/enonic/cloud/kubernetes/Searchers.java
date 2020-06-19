package com.enonic.cloud.kubernetes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;

@Value.Immutable
@Params
public abstract class Searchers
{
    public abstract InformerSearcher<ConfigMap> configMap();

    public abstract InformerSearcher<Ingress> ingress();

    public abstract InformerSearcher<Namespace> namespace();

    public abstract InformerSearcher<Pod> pod();

    public abstract InformerSearcher<Xp7App> xp7App();

    public abstract InformerSearcher<Xp7Config> xp7Config();

    public abstract InformerSearcher<Xp7Deployment> xp7Deployment();

    public abstract InformerSearcher<Xp7VHost> xp7VHost();
}
