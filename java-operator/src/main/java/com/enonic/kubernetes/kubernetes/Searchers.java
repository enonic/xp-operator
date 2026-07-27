package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
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

    InformerSearcher<Xp8Config> xp8Config();

    InformerSearcher<Xp8Deployment> xp8Deployment();

    InformerSearcher<Event> event();
}
