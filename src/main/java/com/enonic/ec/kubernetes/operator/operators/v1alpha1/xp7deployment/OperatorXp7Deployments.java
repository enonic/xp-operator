package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.client.Xp7AppClientProducer;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClientProducer;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client.Xp7VHostClientProducer;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7Deployments
    extends OperatorNamespaced
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    Xp7ConfigClientProducer xp7ConfigClientProducer;

    @Inject
    Xp7AppClientProducer xp7AppClientProducer;

    @Inject
    Xp7VHostClientProducer xp7VHostClientProducer;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

    @ConfigProperty(name = "operator.deployment.xp.preinstall", defaultValue = "")
    List<String> preInstallAppsNames;

    Map<String, String> preInstallApps;

    void onStartup( @Observes StartupEvent _ev )
    {
        preInstallApps = new HashMap<>();
        preInstallAppsNames.forEach( p -> preInstallApps.put( p, cfgStr( "operator.deployment.xp.preinstall." + p ) ) );
        xp7DeploymentCache.addWatcher( this::watch );
    }

    private void watch( final Watcher.Action action, final String id, final Optional<Xp7DeploymentResource> oldResource,
                        final Optional<Xp7DeploymentResource> newResource )
    {
        if ( action == Watcher.Action.DELETED )
        {
            // Do nothing
            return;
        }

        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        runCommands( commandBuilder -> createCommands( commandBuilder, info ) );
    }

    protected void createCommands( ImmutableCombinedCommand.Builder commandBuilder, InfoXp7Deployment info )
    {
        ImmutableCreateXpDeployment.builder().
            defaultClient( defaultClientProducer.client() ).
            configClient( xp7ConfigClientProducer.produce() ).
            appClient( xp7AppClientProducer.produce() ).
            vHostClient( xp7VHostClientProducer.produce() ).
            info( info ).
            preInstallApps( preInstallApps ).
            build().
            addCommands( commandBuilder );
    }
}
