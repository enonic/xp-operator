package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.operators.v1alpha2.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.common.cache.ConfigMapCache;
import com.enonic.ec.kubernetes.operator.common.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.commands.ImmutableCommandXpConfigApplyAll;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info.DiffXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info.ImmutableInfoXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client.Xp7DeploymentCache;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7Config
    extends OperatorNamespaced
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

    @Inject
    Xp7ConfigCache xp7ConfigCache;

    @Inject
    ConfigMapCache configMapCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        xp7ConfigCache.addWatcher( this::watchXpConfig );
    }

    private void watchXpConfig( final Watcher.Action action, final String s, final Optional<Xp7ConfigResource> oldResource,
                                final Optional<Xp7ConfigResource> newResource )
    {
        Optional<ResourceInfoNamespaced<Xp7ConfigResource, DiffXp7Config>> i = getInfo( action, () -> ImmutableInfoXp7Config.builder().
            xpDeploymentCache( xp7DeploymentCache ).
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
                                   ResourceInfoNamespaced<Xp7ConfigResource, DiffXp7Config> info )
    {
        ImmutableCommandXpConfigApplyAll.builder().
            defaultClient( defaultClientProducer.client() ).
            configMapCache( configMapCache ).
            xpConfigCache( xp7ConfigCache ).
            info( info ).
            build().
            addCommands( commandBuilder );
    }
}
