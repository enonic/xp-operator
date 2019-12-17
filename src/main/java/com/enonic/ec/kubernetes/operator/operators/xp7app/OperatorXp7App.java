package com.enonic.ec.kubernetes.operator.operators.xp7app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.xp7app.commands.ImmutableCommandXpAppApplyAll;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7app.client.Xp7AppCache;
import com.enonic.ec.kubernetes.operator.info.xp7app.DiffXp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.info.xp7app.ImmutableInfoXp7App;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7App
    extends OperatorNamespaced
{
    @Inject
    Xp7ConfigClientProducer configClientProducer;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

    @Inject
    Xp7ConfigCache xp7ConfigCache;

    @Inject
    Xp7AppCache xp7AppCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xp7AppCache.addWatcher( this::watchApps );
    }

    private void watchApps( final Watcher.Action action, final String s, final Optional<Xp7AppResource> oldResource,
                            final Optional<Xp7AppResource> newResource )
    {
        Optional<ResourceInfoNamespaced<Xp7AppResource, DiffXp7App>> i = getInfo( action, () -> ImmutableInfoXp7App.builder().
            xpDeploymentCache( xp7DeploymentCache ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {
            // Because multiple apps could potentially be deployed at the same time,
            // lets use the stall function to let them accumulate before we update config
            stallAndRunCommands( ( commandBuilder ) -> createCommands( commandBuilder, info ) );
        } );
    }

    protected void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
                                   ResourceInfoNamespaced<Xp7AppResource, DiffXp7App> info )
    {
        ImmutableCommandXpAppApplyAll.builder().
            xpConfigClient( configClientProducer.produce() ).
            xpConfigCache( xp7ConfigCache ).
            xpAppCache( xp7AppCache ).
            info( info ).
            build().
            addCommands( commandBuilder );
    }
}
