package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.commands.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.client.IssuerClientProducer;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorDeployments
{
    private final static Logger log = LoggerFactory.getLogger( OperatorDeployments.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    IssuerClientProducer issuerClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpDeploymentCache.addWatcher( this::watch );
    }

    private void watch( final Watcher.Action action, final String id, final Optional<XpDeploymentResource> oldResource,
                        final Optional<XpDeploymentResource> newResource )
    {
        if ( action == Watcher.Action.MODIFIED && oldResource.get().equals( newResource.get() ) )
        {
            log.debug( "No change detected" );
            return;
        }

        if ( action == Watcher.Action.DELETED )
        {
            log.info( "XpDeployment with id '" + oldResource.get().getMetadata().getUid() + "' and name '" +
                          oldResource.get().getMetadata().getName() + "' DELETED" );

            // Note that we do not have to do anything on DELETE events because the
            // Kubernetes garbage collector cleans up the deployment for us.
            return;
        }

        try
        {
            ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();

            ImmutableCreateXpDeployment.builder().
                defaultClient( defaultClientProducer.client() ).
                issuerClient( issuerClient.produce() ).
                oldResource( oldResource ).
                newResource( newResource.get() ).
                build().
                addCommands( commandBuilder );

            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            // TODO: What to do in this case? Update XP deployment with info maybe?
            log.error( "Failed creating XP deployment", e );
        }
    }
}
