package com.enonic.ec.kubernetes.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.CombinedKubeCommand;
import com.enonic.ec.kubernetes.deployment.XpDeploymentCache;
import com.enonic.ec.kubernetes.deployment.XpDeploymentClientProducer;
import com.enonic.ec.kubernetes.operator.commands.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

@ApplicationScoped
public class Operator
{
    private final static Logger log = LoggerFactory.getLogger( Operator.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpDeploymentClientProducer xpDeploymentClientProducer;

    @Inject
    IssuerClientProducer issuerClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        log.debug( "Starting operator" );

        log.debug( "Initialize xpDeployment cache" );
        xpDeploymentCache.initialize( xpDeploymentClientProducer.produce().client().list().getItems() );

        xpDeploymentCache.addWatcher( ( action, id, oldResource, newResource ) -> {
            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
            {
                // Only modify if spec has changed
                if ( oldResource.isEmpty() || !oldResource.get().getSpec().equals( newResource.get().getSpec() ) )
                {
                    try
                    {
                        // TODO: What to do if cloud+project of 2 different people have the same name??
                        CombinedKubeCommand command = ImmutableCreateXpDeployment.builder().
                            defaultClient( defaultClientProducer.client() ).
                            issuerClient( issuerClient.produce() ).
                            oldResource( oldResource ).
                            newResource( newResource.get() ).
                            build().
                            execute();

                        command.execute();
                    }
                    catch ( Exception e )
                    {
                        // TODO: What to do in this case? Update XP deployment with info maybe?
                        log.error( "Failed creating XP deployment", e );
                    }
                }

            }
            else if ( action == Watcher.Action.DELETED )
            {
                log.info( "XpDeployment with id '" + oldResource.get().getMetadata().getUid() + "' and name '" +
                              oldResource.get().getMetadata().getName() + "' DELETED" );
                // Note that we do not have to do anything on DELETE events because the
                // Kubernetes garbage collector cleans up the deployment for us.
            }
        } );
    }
}
