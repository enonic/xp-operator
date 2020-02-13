package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.common.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands.ImmutableCommandConfigMapUpdateAll;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.ImmutableInfoXp7Config;

@ApplicationScoped
public class OperatorXp7Config
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7Config.class );

    @Inject
    Clients clients;

    @Inject
    Caches caches;

    void onStartup( @Observes StartupEvent _ev )
    {
        new Thread( () -> stallAndRunCommands( 1000L, () -> {
            log.info( "Started listening for Xp7Config events" );
            caches.getConfigCache().addWatcher( this::watchXpConfig );
        } ) ).start();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchXpConfig( final Watcher.Action action, final String s, final Optional<V1alpha2Xp7Config> oldResource,
                                final Optional<V1alpha2Xp7Config> newResource )
    {
        Optional<ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config>> i = getInfo( action, () -> ImmutableInfoXp7Config.builder().
            caches( caches ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {
            log.info( String.format( "Xp7Config '%s' %s", info.resource().getMetadata().getName(), action ) );

            // Because multiple configs could potentially be deployed at the same time,
            // lets use the stall function to let them accumulate before we update config
            stallAndRunCommands( 1500L, ( commandBuilder ) -> createCommands( commandBuilder, info ) );
        } );
    }

    private void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
                                 ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config> info )
    {
        ImmutableCommandConfigMapUpdateAll.builder().
            clients( clients ).
            caches( caches ).
            info( info ).
            build().
            addCommands( commandBuilder );
    }
}
