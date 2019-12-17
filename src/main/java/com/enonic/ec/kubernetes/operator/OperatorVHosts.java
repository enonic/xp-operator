package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableCommandXpVHostConfigApply;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableCommandXpVHostIngressApply;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.client.Xp7VHostCache;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigClientProducer;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.info.xp7vhost.DiffXp7VHost;
import com.enonic.ec.kubernetes.operator.info.xp7vhost.ImmutableInfoXp7VHost;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorVHosts
    extends OperatorNamespaced
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    Xp7ConfigClientProducer configClientProducer;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

    @Inject
    Xp7VHostCache xp7VHostCache;

    @Inject
    Xp7ConfigCache xp7ConfigCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xp7VHostCache.addWatcher( this::watchVHosts );
    }

    private void watchVHosts( final Watcher.Action action, final String s, final Optional<Xp7VHostResource> oldResource,
                              final Optional<Xp7VHostResource> newResource )
    {
        Optional<ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost>> i = getInfo( action, () -> ImmutableInfoXp7VHost.builder().
            xpDeploymentCache( xp7DeploymentCache ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> createCommands( ImmutableCombinedCommand.builder(), info ) );
    }

    protected void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
                                   ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost> info )
    {
        if ( !info.resourceDeleted() )
        {
            // Only apply ingress on ADD/MODIFY
            ImmutableCommandXpVHostIngressApply.builder().
                defaultClient( defaultClientProducer.client() ).
                info( info ).
                build().
                addCommands( commandBuilder );
        }

        // Because multiple vHosts could potentially be deployed at the same time,
        // lets use the stall function to let them accumulate before we update config
        stallAndRunCommands( commandBuilder, () -> {
            ImmutableCommandXpVHostConfigApply.builder().
                xpConfigClient( configClientProducer.produce() ).
                xpConfigCache( xp7ConfigCache ).
                vHostCache( xp7VHostCache ).
                info( info ).
                build().
                addCommands( commandBuilder );
        } );
    }
}
