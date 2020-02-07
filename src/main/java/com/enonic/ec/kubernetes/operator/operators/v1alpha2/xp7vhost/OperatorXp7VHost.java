package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost;

import java.util.Map;
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
import com.enonic.ec.kubernetes.operator.helm.Helm;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmKubeCmdBuilder;
import com.enonic.ec.kubernetes.operator.operators.common.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.commands.ImmutableCommandXpVHostsApply;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7VHost;

@ApplicationScoped
public class OperatorXp7VHost
    extends OperatorNamespaced
{
    @Inject
    Clients clients;

    @Inject
    Caches caches;

    @Inject
    Helm helm;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

    @Inject
    @Named("baseValues")
    Map<String, Object> baseValues;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getVHostCache().addWatcher( this::watchVHosts );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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

    private void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
                                 ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost> info )
    {
        ImmutableHelmKubeCmdBuilder.builder().
            clients( clients ).
            helm( helm ).
            chart( chartRepository.get( "v1alpha2/xp7vhost" ) ).
            namespace( info.deploymentInfo().namespaceName() ).
            valueBuilder( ImmutableXp7VHostValues.builder().
                baseValues( baseValues ).
                info( info ).
                build() ).
            build().
            addCommands( commandBuilder );

        // Because multiple vhosts could potentially be deployed at the same time, lets use
        // the stall function to let them accumulate in the cache before we update config
        stallAndRunCommands( 500L, commandBuilder, () -> ImmutableCommandXpVHostsApply.builder().
            clients( clients ).
            caches( caches ).
            info( info ).
            build().
            addCommands( commandBuilder ) );
    }
}
