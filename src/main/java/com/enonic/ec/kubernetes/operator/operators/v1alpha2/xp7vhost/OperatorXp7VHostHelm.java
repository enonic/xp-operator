package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
import com.enonic.ec.kubernetes.operator.operators.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.commands.ImmutableCommandXpVHostConfigApply;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.commands.ImmutableCommandXpVHostIngressTemplateApply;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7VHost;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7VHostHelm
    extends OperatorNamespaced
{
    @Inject
    Clients clients;

    @Inject
    Caches caches;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

    void onStartup( @Observes StartupEvent _ev )
    {
        // TODO: Enable
        //xp7VHostCache.addWatcher( this::watchVHosts );
    }

    private void watchVHosts( final Watcher.Action action, final String s, final Optional<V1alpha2Xp7VHost> oldResource,
                              final Optional<V1alpha2Xp7VHost> newResource )
    {
        Optional<ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost>> i = getInfo( action, () -> ImmutableInfoXp7VHost.builder().
            caches( caches ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> createCommands( ImmutableCombinedCommand.builder(), info ) );
    }

    protected void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
                                   ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost> info )
    {

        ImmutableCommandXpVHostIngressTemplateApply.builder().
            clients( clients ).
            info( info ).
            build().
            addCommands( commandBuilder );

        // Because multiple vHosts could potentially be deployed at the same time,
        // lets use the stall function to let them accumulate before we update config
        stallAndRunCommands( commandBuilder, () -> {
            ImmutableCommandXpVHostConfigApply.builder().
                clients( clients ).
                caches( caches ).
                info( info ).
                build().
                addCommands( commandBuilder );
        } );
    }
}
