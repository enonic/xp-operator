package com.enonic.ec.kubernetes.operator.commands.vhosts;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    protected abstract XpConfigClient client();

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

            // Create / Update config
            ImmutableCommandXpVHostConfigNodesApply.builder().
                client( client() ).
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
        // Get all vHosts in this namespace
        List<XpVHostResource> allVHosts = vHostCache().getByNamespace( info.namespace() ).collect( Collectors.toList());

        // Get all mappings in one place
        List<Mapping> mappings = new LinkedList<>();
        allVHosts.forEach( v -> v.getSpec().mappings().forEach( m -> mappings.add( ImmutableMapping.builder().
            host( v.getSpec().host() ).
            node( m.node() ).
            source( m.source() ).
            target( m.target() ).
            idProvider( Optional.ofNullable( m.idProvider() ) ).
            build() ) ) );

        // Get mappings with no node
        List<Mapping> withNoNode = mappings.stream().filter( n -> n.node() == null ).collect( Collectors.toList() );

        // Group mappings by node
        Map<String, List<Mapping>> map =
            mappings.stream().filter( n -> n.node() != null ).collect( Collectors.groupingBy( Mapping::node ) );

        // Add missing nodes to map
        info.xpDeploymentResource().getSpec().nodes().keySet().forEach( k -> {
            if ( !map.containsKey( k ) )
            {
                map.put( k, new LinkedList<>() );
            }
        } );

        // Add no node mappings to all other nodes
        map.forEach( ( node, m ) -> m.addAll( withNoNode ) );

        return map;
    }
}
