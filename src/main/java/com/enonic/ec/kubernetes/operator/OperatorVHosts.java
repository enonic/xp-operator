package com.enonic.ec.kubernetes.operator;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.cache.ConfigMapCache;
import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.crd.vhost.diff.ImmutableDiffSpec;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.client.IssuerClientProducer;
import com.enonic.ec.kubernetes.operator.vhosts.ImmutableXpVHostApplyConfigMap;
import com.enonic.ec.kubernetes.operator.vhosts.ImmutableXpVHostApplyIngress;

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
            ImmutableXpVHostApplyConfigMap.builder().
                client( defaultClientProducer.client() ).
                addConfigMaps( newConfigMap.get() ).
                diffs( xpVHostCache.stream().
                    filter( v -> v.getMetadata().getNamespace().equals( newConfigMap.get().getMetadata().getNamespace() ) ).
                    map( v -> ImmutableDiffSpec.builder().newValue( v.getSpec() ).build() ).
                    collect( Collectors.toList() ) ).
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
            String namespace =
                newVHost.isPresent() ? newVHost.get().getMetadata().getNamespace() : oldVHost.get().getMetadata().getNamespace();

            ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();

            ImmutableXpVHostApplyConfigMap.builder().
                client( defaultClientProducer.client() ).
                configMaps( configMapCache.stream().
                    filter( c -> c.getMetadata().getNamespace().equals( namespace ) ).
                    collect( Collectors.toList() ) ).
                diffs( xpVHostCache.stream().
                    filter( v -> v.getMetadata().getNamespace().equals( namespace ) ).
                    map( v -> ImmutableDiffSpec.builder().newValue( v.getSpec() ).build() ).
                    collect( Collectors.toList() ) ).
                build().
                addCommands( commandBuilder );

            if ( !( action == Watcher.Action.DELETED ) )
            {
                ImmutableXpVHostApplyIngress.builder().
                    defaultClient( defaultClientProducer.client() ).
                    issuerClient( issuerClientProducer.produce() ).
                    namespace( namespace ).
                    ownerReference( createOwnerReference( newVHost.get() ) ).
                    addDiffs( ImmutableDiffSpec.builder().
                        oldValue( Optional.ofNullable( oldVHost.map( XpVHostResource::getSpec ).orElse( null ) ) ).
                        newValue( Optional.ofNullable( newVHost.map( XpVHostResource::getSpec ).orElse( null ) ) ).
                        build() ).
                    build().
                    addCommands( commandBuilder );
            }

            // Note: Kubernetes garbage collector will cleanup old ingress, service and issuers

            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // TODO: Something
        }
    }

    private OwnerReference createOwnerReference( final XpVHostResource resource )
    {
        return new OwnerReference( resource.getApiVersion(), true, true, resource.getKind(), resource.getMetadata().getName(),
                                   resource.getMetadata().getUid() );
    }
}
