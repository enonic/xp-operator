package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.common.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.commands.ImmutableCommandXpAppsApply;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.DiffXp7App;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.ImmutableInfoXp7App;

@ApplicationScoped
public class OperatorXp7App
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7App.class );

    @Inject
    Caches caches;

    @Inject
    Clients clients;

    void onStartup( @Observes StartupEvent _ev )
    {
        new Thread( () -> stallAndRunCommands( 1000L, () -> {
            log.info( "Started listening for Xp7App events" );
            caches.getAppCache().addWatcher( this::watchApps );
        } ) ).start();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchApps( final Watcher.Action action, final String s, final Optional<V1alpha1Xp7App> oldResource,
                            final Optional<V1alpha1Xp7App> newResource )
    {
        // Create info about the CRD
        Optional<ResourceInfoNamespaced<V1alpha1Xp7App, DiffXp7App>> i = getInfo( action, () -> ImmutableInfoXp7App.builder().
            caches( caches ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {
            log.info( String.format( "Xp7App '%s' %s", info.resource().getMetadata().getName(), action ) );

            // Because multiple apps could potentially be deployed at the same time, lets use
            // the stall function to let them accumulate in the cache before we update config
            stallAndRunCommands( 500L, ( commandBuilder ) -> createCommands( commandBuilder, info ) );
        } );
    }

    private void createCommands( ImmutableCombinedCommand.Builder commandBuilder, ResourceInfoNamespaced<V1alpha1Xp7App, DiffXp7App> info )
    {
        ImmutableCommandXpAppsApply.builder().
            clients( clients ).
            caches( caches ).
            info( info ).
            build().
            addCommands( commandBuilder );
    }
}
