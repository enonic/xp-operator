package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
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
    @ConfigProperty(name = "operator.informers.reSync")
    long informerReSync;

    @Singleton
    @Produces
    Informers createInformers( final Clients clients )
    {
        SharedInformerFactory sf = clients.k8s().informers();
        // Todo Fix!
        return InformersImpl.of( clients, sf, configInformer( sf ), ingressInformer( sf ), namespaceInformer( sf ), podInformer( sf ),
                                 xp7AppInformer( clients, sf ), xp7ConfigInformer( clients, sf ), xp7DeploymentInformer( clients, sf ),
                                 xp7VHostInformer( clients, sf ) );
    }

    SharedIndexInformer<ConfigMap> configInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( ConfigMap.class, ConfigMapList.class, informerReSync );
    }

    SharedIndexInformer<Ingress> ingressInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( new CustomResourceDefinitionContext.Builder().
            withGroup( "extensions" ).
            withVersion( "v1beta1" ).
            withName( "Ingress" ).
            withScope( "Namespaced" ).
            withPlural( "ingresses" ).
            build(), Ingress.class, IngressList.class, informerReSync );
    }

    SharedIndexInformer<Namespace> namespaceInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Namespace.class, NamespaceList.class, informerReSync );
    }

    SharedIndexInformer<Pod> podInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Pod.class, PodList.class, informerReSync );
    }

    SharedIndexInformer<Xp7App> xp7AppInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Apps().createContext(), Xp7App.class, Xp7AppList.class, informerReSync );
    }

    SharedIndexInformer<Xp7Config> xp7ConfigInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Configs().createContext(), Xp7Config.class, Xp7ConfigList.class,
                                                        informerReSync );
    }

    SharedIndexInformer<Xp7Deployment> xp7DeploymentInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Deployments().createContext(), Xp7Deployment.class,
                                                        Xp7DeploymentList.class, informerReSync );
    }

    SharedIndexInformer<Xp7VHost> xp7VHostInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7VHosts().createContext(), Xp7VHost.class, Xp7VHostList.class,
                                                        informerReSync );
    }
}
