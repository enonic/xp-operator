package com.enonic.kubernetes.kubernetes;

import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class ClusterIdProducer
{
    private static final Logger log = LoggerFactory.getLogger( ClusterIdProducer.class );

    @Singleton
    @Produces
    @Named("clusterId")
    String createClusterId( Clients clients )
    {
        singletonAssert(this, "createClusterId");
        Namespace ns = clients.k8s().namespaces().withName( "kube-system" ).get();
        if ( ns == null )
        {
            log.warn( "Could not find namespace 'kube-system'. Using random UUID for clusterId" );
            return UUID.randomUUID().toString();
        }
        return ns.getMetadata().getUid();
    }
}
