package com.enonic.ec.kubernetes.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.deployment.CrdClientsProducer;
import com.enonic.ec.kubernetes.deployment.XpDeploymentCache;

@ApplicationScoped
public class Operator
{
    private final Logger log = LoggerFactory.getLogger( Operator.class );

    @Inject
    @Named("default")
    KubernetesClient defaultClient;

    @Inject
    CrdClientsProducer.XpDeploymentClient xpDeploymentClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        log.debug( "Starting operator" );

        log.debug( "Initialize xpDeployment cache" );
        xpDeploymentCache.initialize( xpDeploymentClient.getClient().list().getItems() );

        // TODO: Why are we getting add events on startup?

        xpDeploymentCache.addWatcher( ( action, id, resource ) -> {
            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
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
