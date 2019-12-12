package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.cache.ConfigMapCache;
import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.commands.config.ImmutableCommandXpConfigApplyAll;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.config.diff.ImmutableInfoConfig;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorConfig
    extends OperatorNamespaced
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @Inject
    XpConfigCache xpConfigCache;

    @Inject
    ConfigMapCache configMapCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpConfigCache.addWatcher( this::watchXpConfig );
    }

    private void watchXpConfig( final Watcher.Action action, final String s, final Optional<XpConfigResource> oldResource,
                                final Optional<XpConfigResource> newResource )
    {
        Optional<ResourceInfoNamespaced<XpConfigResource, DiffResource>> i = getInfo( action, () -> ImmutableInfoConfig.builder().
            xpDeploymentCache( xpDeploymentCache ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {
            // Because multiple configs could potentially be deployed at the same time,
            // lets use the stall function to let them accumulate before we update config
            stallAndRunCommands( ( commandBuilder ) -> {
                ImmutableCommandXpConfigApplyAll.builder().
                    defaultClient( defaultClientProducer.client() ).
                    configMapCache( configMapCache ).
                    xpConfigCache( xpConfigCache ).
                    info( info ).
                    build().
                    addCommands( commandBuilder );
            } );
        } );
    }
}
