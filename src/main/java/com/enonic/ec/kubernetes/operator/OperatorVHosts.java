package com.enonic.ec.kubernetes.operator;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.cache.ConfigMapCache;
import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.client.IssuerClientProducer;
import com.enonic.ec.kubernetes.operator.vhosts.ImmutableXpVHostConfigMapOnChange;
import com.enonic.ec.kubernetes.operator.vhosts.ImmutableXpVHostCreate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorVHosts
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( OperatorVHosts.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    IssuerClientProducer issuerClientProducer;

    @Inject
    XpVHostCache xpVHostCache;

    @Inject
    ConfigMapCache configMapCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        configMapCache.addWatcher( this::watchConfigMap );
        xpVHostCache.addWatcher( this::watchVHosts );
    }

    private void watchConfigMap( final Watcher.Action action, final String s, final Optional<ConfigMap> oldConfigMap,
                                 final Optional<ConfigMap> newConfigMap )
    {
        if ( newConfigMap.isEmpty() )
        {
            log.debug( "ConfigMap being deleted" );
            return;
        }

        try
        {
            ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();
            ImmutableXpVHostConfigMapOnChange.builder().
                client( defaultClientProducer.client() ).
                configMap( newConfigMap.get() ).
                xpVHostCache( xpVHostCache ).
                build().
                addCommands( commandBuilder );
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // TODO: Something
        }
    }

    private void watchVHosts( final Watcher.Action action, final String s, final Optional<XpVHostResource> oldVHost,
                              final Optional<XpVHostResource> newVHost )
    {
        if ( action == Watcher.Action.MODIFIED && oldVHost.get().equals( newVHost.get() ) )
        {
            log.debug( "No change detected" );
            return;
        }

        try
        {
            ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();
            ImmutableXpVHostCreate.builder().
                defaultClient( defaultClientProducer.client() ).
                issuerClient( issuerClientProducer.produce() ).
                oldResource( oldVHost ).
                newResource( newVHost ).
                build().
                addCommands( commandBuilder );
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // TODO: Something
        }
    }
}
