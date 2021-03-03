package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressList;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import com.enonic.cloud.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.cloud.kubernetes.client.v1alpha2.Domain;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;


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
            eventInformer( eventInformer( sf ) ).
            xp7AppInformer( xp7AppInformer( sf ) ).
            xp7ConfigInformer( xp7ConfigInformer( sf ) ).
            xp7DeploymentInformer( xp7DeploymentInformer( sf ) ).
            domainInformer( domainInformer( sf ) ).
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

    private SharedIndexInformer<Event> eventInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Event.class, EventList.class, informerReSync );
    }

    private SharedIndexInformer<Xp7App> xp7AppInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( Xp7App.createCrdContext(), Xp7App.class, Xp7App.Xp7AppList.class, informerReSync );
    }

    private SharedIndexInformer<Xp7Config> xp7ConfigInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( Xp7Config.createCrdContext(), Xp7Config.class, Xp7Config.Xp7ConfigList.class,
                                                        informerReSync );
    }

    private SharedIndexInformer<Xp7Deployment> xp7DeploymentInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( Xp7Deployment.createCrdContext(), Xp7Deployment.class,
                                                        Xp7Deployment.Xp7DeploymentList.class, informerReSync );
    }

    private SharedIndexInformer<Domain> domainInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerForCustomResource( Domain.createCrdContext(), Domain.class, Domain.DomainList.class, informerReSync );
    }
}
