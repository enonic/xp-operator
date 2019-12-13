package com.enonic.ec.kubernetes.operator;

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

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.commands.deployments.ImmutableCreateXpDeployment;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppClientProducer;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.ImmutableInfoDeployment;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.InfoDeployment;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostClientProducer;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorDeployments
    extends OperatorNamespaced
{
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
        if ( action == Watcher.Action.DELETED )
        {
            // Do nothing
            return;
        }

        InfoDeployment info = ImmutableInfoDeployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        runCommands( commandBuilder -> {
            ImmutableCreateXpDeployment.builder().
                defaultClient( defaultClientProducer.client() ).
                configClient( xpConfigClientProducer.produce() ).
                appClient( xpAppClientProducer.produce() ).
                vHostClient( xpVHostClientProducer.produce() ).
                info( info ).
                preInstallApps( preInstallApps ).
                build().
                addCommands( commandBuilder );
        } );
    }
}
