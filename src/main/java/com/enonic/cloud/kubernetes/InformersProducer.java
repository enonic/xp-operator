package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressList;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppList;
import com.enonic.cloud.kubernetes.client.v1alpha2.domain.DomainList;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7config.Xp7ConfigList;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentList;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;

public class InformersProducer
{
    private static final Logger log = LoggerFactory.getLogger( InformersProducer.class );

    @ConfigProperty(name = "operator.informers.reSync")
    long informerReSync;

    @Singleton
    @Produces
    Informers createInformers( final Clients clients )
    {
        SharedInformerFactory sf = clients.k8s().informers();

        sf.addSharedInformerEventListener( e -> log.error( "Informer exception: " + e.getMessage(), e ) );

        return InformersImpl.builder().
            clients( clients ).
            informerFactory( sf ).
            configMapInformer( configInformer( sf ) ).
            ingressInformer( ingressInformer( sf ) ).
            namespaceInformer( namespaceInformer( sf ) ).
            podInformer( podInformer( sf ) ).
            xp7AppInformer( xp7AppInformer( clients, sf ) ).
            xp7ConfigInformer( xp7ConfigInformer( clients, sf ) ).
            xp7DeploymentInformer( xp7DeploymentInformer( clients, sf ) ).
            domainInformer( domainInformer( clients, sf ) ).
            build();
    }

    private SharedIndexInformer<ConfigMap> configInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( ConfigMap.class, ConfigMapList.class, informerReSync );
    }

    private SharedIndexInformer<Ingress> ingressInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Ingress.class, IngressList.class, informerReSync );
    }

    private SharedIndexInformer<Namespace> namespaceInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Namespace.class, NamespaceList.class, informerReSync );
    }

    private SharedIndexInformer<Pod> podInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Pod.class, PodList.class, informerReSync );
    }

    private SharedIndexInformer<Xp7App> xp7AppInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Apps().createContext(), Xp7App.class, Xp7AppList.class, informerReSync );
    }

    private SharedIndexInformer<Xp7Config> xp7ConfigInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Configs().createContext(), Xp7Config.class, Xp7ConfigList.class,
                                                        informerReSync );
    }

    private SharedIndexInformer<Xp7Deployment> xp7DeploymentInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.xp7Deployments().createContext(), Xp7Deployment.class,
                                                        Xp7DeploymentList.class, informerReSync );
    }

    private SharedIndexInformer<Domain> domainInformer( final Clients clients, final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( clients.domain().createContext(), Domain.class, DomainList.class, informerReSync );
    }
}
