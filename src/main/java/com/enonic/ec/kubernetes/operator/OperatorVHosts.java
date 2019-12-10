package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableCommandXpVHostConfigApply;
import com.enonic.ec.kubernetes.operator.commands.vhosts.ImmutableCommandXpVHostIngressApply;
import com.enonic.ec.kubernetes.operator.crd.ImmutableXpCrdInfo;
import com.enonic.ec.kubernetes.operator.crd.XpCrdInfo;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.diff.ImmutableDiffResource;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorVHosts
    extends OperatorStall
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

    @ConfigProperty(name = "operator.config.xp.vhosts.file")
    String xp7ConfigFile;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpVHostCache.addWatcher( this::watchVHosts );
    }

    private void watchVHosts( final Watcher.Action action, final String s, final Optional<XpVHostResource> oldVHost,
                              final Optional<XpVHostResource> newVHost )
    {
        XpCrdInfo info = ImmutableXpCrdInfo.builder().
            cache( xpDeploymentCache ).
            oldResource( oldVHost ).
            newResource( newVHost ).
            build();

        DiffResource diff = ImmutableDiffResource.builder().
            oldValue( oldVHost ).
            newValue( newVHost ).
            build();

        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

        if ( action != Watcher.Action.DELETED )
        {
            // Only apply ingress on ADD/MODIFY
            ImmutableCommandXpVHostIngressApply.builder().
                defaultClient( defaultClientProducer.client() ).
                info( info ).
                diff( diff ).
                build().
                addCommands( commandBuilder );
        }

        // Because multiple vHosts could potentially be deployed at the same time,
        // lets use the stall function to let them accumulate before we update config
        stall( () -> {
            ImmutableCommandXpVHostConfigApply.builder().
                client( configClientProducer.produce() ).
                xpConfigCache( xpConfigCache ).
                vHostCache( xpVHostCache ).
                info( info ).
                build().
                addCommands( commandBuilder );
            try
            {
                commandBuilder.build().execute();
            }
            catch ( Exception e )
            {
                log.error( "Unable to update apps", e );
            }
        } );
    }

    private Optional<XpConfigResource> getNodeVHostConfig( XpCrdInfo info, String node, String nodeVHostConfigName )
    {
        return xpConfigCache.stream().
            filter( c -> c.getMetadata().getNamespace().equals( info.namespace() ) ).
            filter( c -> c.getMetadata().getName().equals( nodeVHostConfigName ) ).
            filter( c -> node.equals( c.getSpec().node() ) ).
            findAny();
    }

}
