package com.enonic.kubernetes.kubernetes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import com.enonic.kubernetes.common.annotations.Params;
import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;


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

    public abstract SharedIndexInformer<Domain> domainInformer();

    public abstract SharedIndexInformer<Event> eventInformer();
}
