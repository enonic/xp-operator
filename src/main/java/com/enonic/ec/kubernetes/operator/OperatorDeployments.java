package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.deployments.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.crd.deployment.ImmutableXpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.ImmutableDiffResource;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorDeployments
{
    private final static Logger log = LoggerFactory.getLogger( OperatorDeployments.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpDeploymentCache.addWatcher( this::watch );
    }

    private void watch( final Watcher.Action action, final String id, final Optional<XpDeploymentResource> oldResource,
                        final Optional<XpDeploymentResource> newResource )
    {
        DiffResource diffResource = ImmutableDiffResource.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();

        if ( diffResource.diffSpec().shouldAddOrModify() )
        {
            try
            {
                ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

                ImmutableCreateXpDeployment.builder().
                    defaultClient( defaultClientProducer.client() ).
                    deploymentName( newResource.get().getMetadata().getName() ).
                    defaultLabels( newResource.get().getMetadata().getLabels() ).
                    namingHelper( ImmutableXpDeploymentNamingHelper.builder().resource( newResource.get() ).build() ).
                    diffSpec( diffResource.diffSpec() ).
                    ownerReference( createOwnerReference( newResource.get() ) ).
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

    private OwnerReference createOwnerReference( final XpDeploymentResource resource )
    {
        return new OwnerReference( resource.getApiVersion(), true, true, resource.getKind(), resource.getMetadata().getName(),
                                   resource.getMetadata().getUid() );
    }
}
