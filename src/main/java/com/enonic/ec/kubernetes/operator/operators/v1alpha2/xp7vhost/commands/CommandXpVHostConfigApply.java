package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.commands;

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
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.helpers.ImmutableMapping;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.helpers.Mapping;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;

@Value.Immutable
public abstract class CommandXpVHostConfigApply
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract Caches caches();

    protected abstract ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost> info();

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
                clients( clients() ).
                caches( caches() ).
                info( info() ).
                name( cfgStrFmt( "operator.config.xp.vhosts.name", nodeName ) ).
                file( cfgStr( "operator.config.xp.vhosts.file" ) ).
                nodeGroup( nodeName ).
                mappings( mappings ).
                build().
                addCommands( commandBuilder );
        }
    }

    private Map<String, List<Mapping>> getNodeMappings( ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost> info )
    {
        // Collect all mappings
        List<Mapping> mappings = new LinkedList<>();

        caches().getvHostCache().getByNamespace( info.deploymentInfo().namespaceName() ).
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
        info().deploymentInfo().resource().getSpec().nodeGroups().keySet().forEach( n -> result.put( n, new LinkedList<>() ) );

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
