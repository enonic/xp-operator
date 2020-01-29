package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands.ImmutableCommandXpConfigApplyAll;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.ImmutableInfoXp7Config;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7Config
    extends OperatorNamespaced
{
    @Inject
    Clients clients;

    @Inject
    Caches caches;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getConfigCache().addWatcher( this::watchXpConfig );
    }

    private void watchXpConfig( final Watcher.Action action, final String s, final Optional<V1alpha2Xp7Config> oldResource,
                                final Optional<V1alpha2Xp7Config> newResource )
    {
        Optional<ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config>> i = getInfo( action, () -> ImmutableInfoXp7Config.builder().
            caches( caches ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {
            // Because multiple configs could potentially be deployed at the same time,
            // lets use the stall function to let them accumulate before we update config
            stallAndRunCommands( ( commandBuilder ) -> createCommands( commandBuilder, info ) );
        } );
    }

    protected void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
                                   ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config> info )
    {
        ImmutableCommandXpConfigApplyAll.builder().
            clients( clients ).
            caches( caches ).
            info( info ).
            build().
            addCommands( commandBuilder );
    }
}
