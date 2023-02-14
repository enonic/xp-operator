package com.enonic.kubernetes.kubernetes;

import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;


public class InformersProducer
{
    private static final Logger log = LoggerFactory.getLogger( InformersProducer.class );

    @ConfigProperty(name = "operator.informers.reSync")
    long informerReSync;

    @Singleton
    @Produces
    Informers createInformers( final Clients clients )
    {
        singletonAssert( this, "createInformers" );

        SharedInformerFactory sf = clients.k8s().informers();

        return InformersImpl.builder()
            .clients( clients )
            .informerFactory( sf )
            .configMapInformer( configInformer( sf ) )
            .ingressInformer( ingressInformer( sf ) )
            .namespaceInformer( namespaceInformer( sf ) )
            .podInformer( podInformer( sf ) )
            .eventInformer( eventInformer( sf ) )
            .xp7AppInformer( xp7AppInformer( sf ) )
            .xp7ConfigInformer( xp7ConfigInformer( sf ) )
            .xp7DeploymentInformer( xp7DeploymentInformer( sf ) )
            .domainInformer( domainInformer( sf ) )
            .build();
    }

    private SharedIndexInformer<ConfigMap> configInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( ConfigMap.class, informerReSync );
    }

    private SharedIndexInformer<Ingress> ingressInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Ingress.class, informerReSync );
    }

    private SharedIndexInformer<Namespace> namespaceInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Namespace.class, informerReSync );
    }

    private SharedIndexInformer<Pod> podInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Pod.class, informerReSync );
    }

    private SharedIndexInformer<Event> eventInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Event.class, informerReSync );
    }

    private SharedIndexInformer<Xp7App> xp7AppInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Xp7App.class, informerReSync );
    }

    private SharedIndexInformer<Xp7Config> xp7ConfigInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Xp7Config.class, informerReSync );
    }

    private SharedIndexInformer<Xp7Deployment> xp7DeploymentInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Xp7Deployment.class, informerReSync );
    }

    private SharedIndexInformer<Domain> domainInformer( final SharedInformerFactory sf )
    {
        return sf.sharedIndexInformerFor( Domain.class, informerReSync );
    }
}
