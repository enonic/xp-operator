package com.enonic.cloud.kubernetes;

import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;

public class ClusterIdProducer
{
    private static final Logger log = LoggerFactory.getLogger( ClusterIdProducer.class );

    @Singleton
    @Produces
    @Named("clusterId")
    String produceClusterId( Clients clients )
    {
        Namespace ns = clients.k8s().namespaces().withName( "kube-system" ).get();
        if ( ns == null )
        {
            log.warn( "Could not find namespace 'kube-system'. Using random UUID for clusterId" );
            return UUID.randomUUID().toString();
        }
        return ns.getMetadata().getUid();
    }
}
