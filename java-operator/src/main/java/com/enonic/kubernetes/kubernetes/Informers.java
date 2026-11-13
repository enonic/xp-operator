package com.enonic.kubernetes.kubernetes;

import org.jspecify.annotations.NullMarked;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import java.util.HashMap;
import java.util.Map;


@NullMarked
public record Informers(
    Clients clients,
    SharedInformerFactory informerFactory,
    SharedIndexInformer<ConfigMap> configMapInformer,
    SharedIndexInformer<Ingress> ingressInformer,
    SharedIndexInformer<Namespace> namespaceInformer,
    SharedIndexInformer<Pod> podInformer,
    SharedIndexInformer<Xp8Config> xp8ConfigInformer,
    SharedIndexInformer<Xp8Deployment> xp8DeploymentInformer,
    SharedIndexInformer<Event> eventInformer )
{
    public Map<Class<? extends HasMetadata>, SharedIndexInformer<?>> allInformers()
    {
        Map<Class<? extends HasMetadata>, SharedIndexInformer<?>> res = new HashMap<>();
        res.put( ConfigMap.class, configMapInformer() );
        res.put( Ingress.class, ingressInformer() );
        res.put( Namespace.class, namespaceInformer() );
        res.put( Pod.class, podInformer() );
        res.put( Xp8Config.class, xp8ConfigInformer() );
        res.put( Xp8Deployment.class, xp8DeploymentInformer() );
        res.put( Event.class, eventInformer() );
        return res;
    }
}
