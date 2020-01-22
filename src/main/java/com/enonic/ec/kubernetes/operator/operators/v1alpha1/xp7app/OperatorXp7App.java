package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.commands.ImmutableCommandXpAppApplyAll;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.DiffXp7App;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.ImmutableInfoXp7App;
import com.enonic.ec.kubernetes.operator.operators.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7App
    extends OperatorNamespaced
{
    @Inject
    Caches caches;

    @Inject
    Clients clients;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getAppCache().addWatcher( this::watchApps );
    }

    private void watchApps( final Watcher.Action action, final String s, final Optional<V1alpha1Xp7App> oldResource,
                            final Optional<V1alpha1Xp7App> newResource )
    {
        Optional<ResourceInfoNamespaced<V1alpha1Xp7App, DiffXp7App>> i = getInfo( action, () -> ImmutableInfoXp7App.builder().
            caches( caches ).
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
                                   ResourceInfoNamespaced<V1alpha1Xp7App, DiffXp7App> info )
    {
        ImmutableCommandXpAppApplyAll.builder().
            clients( clients ).
            caches( caches ).
            info( info ).
            build().
            addCommands( commandBuilder );
    }
}
