package com.enonic.ec.kubernetes.operator;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.XpDeploymentCache;
import com.enonic.ec.kubernetes.deployment.XpDeploymentClientProducer;
import com.enonic.ec.kubernetes.operator.commands.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.commands.ImmutablePrePullImages;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

@SuppressWarnings("OptionalGetWithoutIsPresent")
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

    @ConfigProperty(name = "operator.prePull.versions", defaultValue = "")
    List<String> imageVersionPrePull;

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
            else if ( action == Watcher.Action.DELETED )
            {
                log.info( "XpDeployment with id '" + oldResource.get().getMetadata().getUid() + "' and name '" +
                              oldResource.get().getMetadata().getName() + "' DELETED" );
                // Note that we do not have to do anything on DELETE events because the
                // Kubernetes garbage collector cleans up the deployment for us.
            }
        } );

        if ( imageVersionPrePull.size() > 0 )
        {
            log.info( "Pre-Pulling images in cluster for versions: " + imageVersionPrePull );
            try
            {
                ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();

                ImmutablePrePullImages.builder().
                    defaultClient( defaultClientProducer.client() ).
                    addAllVersions( imageVersionPrePull ).
                    build().
                    addCommands( commandBuilder );

                commandBuilder.build().execute();
            }
            catch ( Exception e )
            {
                log.error( "Failed to pre pull images", e );
            }
        }
    }
}
