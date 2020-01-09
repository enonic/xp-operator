package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.commands;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.commands.helpers.ImmutableMapping;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.commands.helpers.Mapping;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client.Xp7VHostCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.DiffXp7VHost;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;

@Value.Immutable
public abstract class CommandXpVHostConfigApply
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract Xp7ConfigClient xpConfigClient();

    protected abstract Xp7ConfigCache xpConfigCache();

    protected abstract Xp7VHostCache vHostCache();

    protected abstract ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Iterate over each node mapping
        for ( Map.Entry<String, List<Mapping>> e : getNodeMappings( info() ).entrySet() )
        {
            String nodeName = e.getKey();
            List<Mapping> mappings = e.getValue();
            mappings.sort( Comparator.comparing( Mapping::host ) );

            // TODO: Setup owner references to Xp7Config to garbage collect

            // Create / Update config
            ImmutableCommandXpVHostConfigNodesApply.builder().
                xpConfigClient( xpConfigClient() ).
                xpConfigCache( xpConfigCache() ).
                info( info() ).
                name( cfgStrFmt( "operator.config.xp.vhosts.name", nodeName ) ).
                file( cfgStr( "operator.config.xp.vhosts.file" ) ).
                node( nodeName ).
                mappings( mappings ).
                build().
                addCommands( commandBuilder );
        }
    }

    private Map<String, List<Mapping>> getNodeMappings( ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost> info )
    {
        // Collect all mappings
        List<Mapping> mappings = new LinkedList<>();
        vHostCache().getByNamespace( info.deploymentInfo().namespaceName() ).
            forEach( v -> v.getSpec().mappings().
                forEach( m -> mappings.add( ImmutableMapping.builder().
                    host( v.getSpec().host() ).
                    node( m.node() ).
                    source( m.source() ).
                    target( m.target() ).
                    idProvider( Optional.ofNullable( m.idProvider() ) ).
                    build() ) ) );

        // Create map for all nodes
        Map<String, List<Mapping>> result = new HashMap<>();

        // Populate map with all the nodes
        info().deploymentInfo().resource().getSpec().nodes().keySet().forEach( n -> result.put( n, new LinkedList<>() ) );

        // Add mappings to nodes
        mappings.forEach( m -> {
            if ( m.node().equals( cfgStr( "operator.deployment.xp.allNodes" ) ) )
            {
                result.values().forEach( v -> v.add( m ) );
            }
            else
            {
                result.get( m.node() ).add( m );
            }
        } );

        return result;
    }
}
