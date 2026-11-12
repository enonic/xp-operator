package com.enonic.kubernetes.kubernetes;

import org.jspecify.annotations.NullMarked;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

@NullMarked
public record Searchers(
    InformerSearcher<ConfigMap> configMap,
    InformerSearcher<Ingress> ingress,
    InformerSearcher<Namespace> namespace,
    InformerSearcher<Pod> pod,
    InformerSearcher<Xp8Config> xp8Config,
    InformerSearcher<Xp8Deployment> xp8Deployment,
    InformerSearcher<Event> event )
{
}
