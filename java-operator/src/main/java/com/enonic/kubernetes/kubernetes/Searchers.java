package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.common.annotations.Params;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.immutables.value.Value;

@Value.Immutable
@Params
public interface Searchers
{
    InformerSearcher<ConfigMap> configMap();

    InformerSearcher<Ingress> ingress();

    InformerSearcher<Namespace> namespace();

    InformerSearcher<Pod> pod();

    InformerSearcher<Xp7App> xp7App();

    InformerSearcher<Xp7Config> xp7Config();

    InformerSearcher<Xp7Deployment> xp7Deployment();

    InformerSearcher<Domain> domain();

    InformerSearcher<Event> event();
}
