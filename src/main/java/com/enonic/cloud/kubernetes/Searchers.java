package com.enonic.cloud.kubernetes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.cloud.kubernetes.client.v1alpha2.Domain;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;

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

    public abstract InformerSearcher<Domain> domain();

    public abstract InformerSearcher<Event> event();
}
