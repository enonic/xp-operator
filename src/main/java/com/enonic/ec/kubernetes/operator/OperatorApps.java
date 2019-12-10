package com.enonic.ec.kubernetes.operator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.apps.ImmutableXpAppsApply;
import com.enonic.ec.kubernetes.operator.crd.ImmutableXpCrdInfo;
import com.enonic.ec.kubernetes.operator.crd.XpCrdInfo;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppCache;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorApps
    extends OperatorStall
{
    private final static Logger log = LoggerFactory.getLogger( OperatorApps.class );

    @Inject
    XpConfigClientProducer configClientProducer;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @Inject
    XpConfigCache xpConfigCache;

    @Inject
    XpAppCache xpAppCache;

    @ConfigProperty(name = "operator.config.xp.deploy.name")
    String xp7ConfigName;

    @ConfigProperty(name = "operator.config.xp.deploy.file")
    String xp7ConfigFile;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpAppCache.addWatcher( this::watchApps );
    }

    private void watchApps( final Watcher.Action action, final String s, final Optional<XpAppResource> oldApp,
                            final Optional<XpAppResource> newApp )
    {
        XpCrdInfo info = ImmutableXpCrdInfo.builder().
            cache( xpDeploymentCache ).
            oldResource( oldApp ).
            newResource( newApp ).
            build();

        // Because multiple apps could potentially be deployed at the same time,
        // lets use the stall function to let them accumulate before we update config
        stall( () -> {

            // Get all apps in namespace
            List<XpAppResource> allApps = xpAppCache.getByNamespace( info.namespace() );

            try
            {
                ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

                // Create / Update apps config
                ImmutableXpAppsApply.builder().
                    client( configClientProducer.produce() ).
                    xpConfigCache( xpConfigCache ).
                    info( info ).
                    name( xp7ConfigName ).
                    file( xp7ConfigFile ).
                    xpAppResources( allApps ).
                    build().
                    addCommands( commandBuilder );

                commandBuilder.build().execute();
            }
            catch ( Exception e )
            {
                log.error( "Unable to update apps", e );
            }
        } );
    }
}
