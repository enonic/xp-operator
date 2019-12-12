package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.commands.apps.ImmutableCommandXpAppApplyAll;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppCache;
import com.enonic.ec.kubernetes.operator.crd.app.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.app.diff.ImmutableInfoApp;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClientProducer;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorApps
    extends OperatorNamespaced
{
    private final static Logger log = LoggerFactory.getLogger( OperatorApps.class );

    @Inject
    XpConfigClientProducer configClientProducer;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @Inject
    XpConfigCache xpConfigCache;

    @Inject
    XpAppCache xpAppCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpAppCache.addWatcher( this::watchApps );
    }

    private void watchApps( final Watcher.Action action, final String s, final Optional<XpAppResource> oldApp,
                            final Optional<XpAppResource> newApp )
    {
        Optional<ResourceInfoNamespaced<XpAppResource, DiffResource>> i = getInfo( action, () -> ImmutableInfoApp.builder().
            xpDeploymentCache( xpDeploymentCache ).
            oldResource( oldApp ).
            newResource( newApp ).
            build() );

        i.ifPresent( info -> {
            // Because multiple apps could potentially be deployed at the same time,
            // lets use the stall function to let them accumulate before we update config
            stallAndRunCommands( ( commandBuilder ) -> {
                ImmutableCommandXpAppApplyAll.builder().
                    client( configClientProducer.produce() ).
                    xpConfigCache( xpConfigCache ).
                    xpAppCache( xpAppCache ).
                    info( info ).
                    build().
                    addCommands( commandBuilder );
            } );
        } );
    }
}
