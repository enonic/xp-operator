package com.enonic.ec.kubernetes.operator.commands.vhosts;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.commands.vhosts.helpers.ImmutableMapping;
import com.enonic.ec.kubernetes.operator.commands.vhosts.helpers.Mapping;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@Value.Immutable
public abstract class CommandXpVHostConfigApply
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract XpConfigClient xpConfigClient();

    protected abstract XpConfigCache xpConfigCache();

    protected abstract XpVHostCache vHostCache();

    protected abstract ResourceInfoNamespaced<XpVHostResource, DiffResource> info();

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

    private Map<String, List<Mapping>> getNodeMappings( ResourceInfoNamespaced<XpVHostResource, DiffResource> info )
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
