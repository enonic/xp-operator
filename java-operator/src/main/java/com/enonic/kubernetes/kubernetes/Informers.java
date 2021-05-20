package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.common.annotations.Params;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;


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

    @Value.Derived
    public Map<Class<? extends HasMetadata>, SharedIndexInformer> allInformers()
    {
        Map<Class<? extends HasMetadata>, SharedIndexInformer> res = new HashMap<>();
        res.put( ConfigMap.class, configMapInformer() );
        res.put( Ingress.class, ingressInformer() );
        res.put( Namespace.class, namespaceInformer() );
        res.put( Pod.class, podInformer() );
        res.put( Xp7App.class, xp7AppInformer() );
        res.put( Xp7Config.class, xp7ConfigInformer() );
        res.put( Xp7Deployment.class, xp7DeploymentInformer() );
        res.put( Domain.class, domainInformer() );
        res.put( Event.class, eventInformer() );
        return res;
    }
}
