package com.enonic.ec.kubernetes.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.crd.CrdClientsProducer;
import com.enonic.ec.kubernetes.common.crd.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.commands.CommandDeployXP;

@ApplicationScoped
public class Operator
{
    private final Logger log = LoggerFactory.getLogger( Operator.class );

    @Inject
    KubernetesClient defaultClient;

    @Inject
    CrdClientsProducer.XpDeploymentClient xpDeploymentClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        log.debug( "Starting operator" );

        // We do this so we do not get old ADD events during startup
        log.debug( "Initialize xpDeployment cache" );
        xpDeploymentCache.initialize( xpDeploymentClient.getClient().list().getItems() );

        xpDeploymentCache.addWatcher( ( action, id, resource ) -> {
            if ( action == Watcher.Action.ADDED )
            {
                CommandDeployXP.newBuilder().
                    client( defaultClient ).
                    resource( resource ).
                    build().
                    execute();
            }
            else if ( action == Watcher.Action.DELETED )
            {
                log.info( "XpDeployment with id '" + resource.getMetadata().getUid() + "' and name '" + resource.getMetadata().getName() +
                              "' DELETED" );
                // Note that we do not have to do anything on DELETE events because the
                // Kubernetes garbage collector cleans up the deployment for us.
            }

        } );
    }
}
