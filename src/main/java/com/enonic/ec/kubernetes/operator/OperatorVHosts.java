package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableCommandXpVHostConfigApply;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableCommandXpVHostIngressApply;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.diff.ImmutableInfoVHost;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorVHosts
    extends OperatorNamespaced
{
    private final static Logger log = LoggerFactory.getLogger( OperatorVHosts.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpConfigClientProducer configClientProducer;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @Inject
    XpVHostCache xpVHostCache;

    @Inject
    XpConfigCache xpConfigCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpVHostCache.addWatcher( this::watchVHosts );
    }

    private void watchVHosts( final Watcher.Action action, final String s, final Optional<XpVHostResource> oldVHost,
                              final Optional<XpVHostResource> newVHost )
    {
        Optional<ResourceInfoNamespaced<XpVHostResource, DiffResource>> i = getInfo( action, () -> ImmutableInfoVHost.builder().
            xpDeploymentCache( xpDeploymentCache ).
            oldResource( oldVHost ).
            newResource( newVHost ).
            build() );

        i.ifPresent( info -> {
            ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

            if ( action != Watcher.Action.DELETED )
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
                    client( configClientProducer.produce() ).
                    xpConfigCache( xpConfigCache ).
                    vHostCache( xpVHostCache ).
                    info( info ).
                    build().
                    addCommands( commandBuilder );
            } );
        } );
    }
}
