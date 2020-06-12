package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppList;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7config.Xp7ConfigList;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentList;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7vhost.Xp7VHostList;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;

public class InformersProducer
{
    private static final Long informerReSync = 30 * 1000L; // TODO: Add to properties file

    @Singleton
    @Produces
    SharedInformerFactory sharedIndexInformerFactory( final Clients clients )
    {
        return clients.k8s().informers();
    }

    @Singleton
    @Produces
    SharedIndexInformer<ConfigMap> configMapSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( ConfigMap.class, ConfigMapList.class, informerReSync );
    }

    @Singleton
    @Produces
    SharedIndexInformer<Ingress> ingressSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Ingress.class, IngressList.class, informerReSync );
    }

    @Singleton
    @Produces
    SharedIndexInformer<Pod> podSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Pod.class, PodList.class, informerReSync );
    }

    @Singleton
    @Produces
    SharedIndexInformer<Xp7App> xp7AppSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Apps().createContext(), Xp7App.class, Xp7AppList.class, informerReSync );
    }

    @Singleton
    @Produces
    SharedIndexInformer<Xp7Config> xp7ConfigSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Configs().createContext(), Xp7Config.class, Xp7ConfigList.class,
                                                        informerReSync );
    }

    @Singleton
    @Produces
    SharedIndexInformer<Xp7Deployment> xp7DeploymentSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Deployments().createContext(), Xp7Deployment.class,
                                                        Xp7DeploymentList.class, informerReSync );
    }

    @Singleton
    @Produces
    SharedIndexInformer<Xp7VHost> xp7VHostSharedIndexInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7VHosts().createContext(), Xp7VHost.class, Xp7VHostList.class,
                                                        informerReSync );
    }
}
