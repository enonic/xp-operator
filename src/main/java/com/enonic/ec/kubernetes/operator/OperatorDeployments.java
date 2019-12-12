package com.enonic.ec.kubernetes.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.deployments.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppClientProducer;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.deployment.ImmutableXpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.ImmutableInfoDeployment;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostClientProducer;
import com.enonic.ec.kubernetes.operator.info.ResourceInfo;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorDeployments
    extends OperatorNamespaced
{
    private final static Logger log = LoggerFactory.getLogger( OperatorDeployments.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpConfigClientProducer xpConfigClientProducer;

    @Inject
    XpAppClientProducer xpAppClientProducer;

    @Inject
    XpVHostClientProducer xpVHostClientProducer;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @ConfigProperty(name = "operator.deployment.xp.preinstall", defaultValue = "")
    List<String> preInstallAppsNames;

    Map<String, String> preInstallApps;

    void onStartup( @Observes StartupEvent _ev )
    {
        preInstallApps = new HashMap<>();
        preInstallAppsNames.forEach( p -> preInstallApps.put( p, cfgStr( "operator.deployment.xp.preinstall." + p ) ) );
        xpDeploymentCache.addWatcher( this::watch );
    }

    private void watch( final Watcher.Action action, final String id, final Optional<XpDeploymentResource> oldResource,
                        final Optional<XpDeploymentResource> newResource )
    {
        ResourceInfo<XpDeploymentResource, DiffResource> info = ImmutableInfoDeployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        if ( info.diff().diffSpec().shouldAddOrModify() )
        {
            try
            {
                ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

                ImmutableCreateXpDeployment.builder().
                    defaultClient( defaultClientProducer.client() ).
                    configClient( xpConfigClientProducer.produce() ).
                    appClient( xpAppClientProducer.produce() ).
                    vHostClient( xpVHostClientProducer.produce() ).
                    resource( newResource.get() ).
                    deploymentName( newResource.get().getMetadata().getName() ).
                    defaultLabels( newResource.get().getMetadata().getLabels() ).
                    namingHelper( ImmutableXpDeploymentNamingHelper.builder().resource( newResource.get() ).build() ).
                    diffSpec( info.diff().diffSpec() ).
                    ownerReference( info.ownerReference() ).
                    preInstallApps( preInstallApps ).
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


}
