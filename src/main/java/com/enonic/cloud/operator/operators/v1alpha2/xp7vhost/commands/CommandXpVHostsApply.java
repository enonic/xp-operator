package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.commands;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.cloud.operator.common.commands.CombinedCommandBuilder;
import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.helpers.ImmutableMapping;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.helpers.Mapping;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.common.Configuration.cfgStrFmt;

@Value.Immutable
public abstract class CommandXpVHostsApply
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

            // Create / Update config
            ImmutableCommandXpUpdateVHostConfigFile.builder().
                clients( clients() ).
                caches( caches() ).
                info( info() ).
                name( cfgStrFmt( "operator.deployment.xp.config.vhosts.nameTemplate", nodeName ) ).
                file( cfgStr( "operator.deployment.xp.config.vhosts.file" ) ).
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

        caches().getVHostCache().getByNamespace( info.deploymentInfo().namespaceName() ).
            forEach( v -> v.getSpec().mappings().
                forEach( m -> mappings.add( ImmutableMapping.builder().
                    host( v.getSpec().host() ).
                    nodeGroup( m.nodeGroup() ).
                    source( m.source() ).
                    target( m.target() ).
                    idProviders( buildIdProviderMap( m ) ).
                    build() ) ) );

        // Create map for all nodes
        Map<String, List<Mapping>> result = new HashMap<>();

        // Populate map with all the nodes
        info().deploymentInfo().resource().getSpec().nodeGroups().keySet().forEach( n -> result.put( n, new LinkedList<>() ) );

        // Add mappings to nodes
        mappings.forEach( m -> {
            if ( m.nodeGroup().equals( cfgStr( "operator.deployment.xp.allNodesKey" ) ) )
            {
                result.values().forEach( v -> v.add( m ) );
            }
            else
            {
                result.get( m.nodeGroup() ).add( m );
            }
        } );

        return result;
    }

    private Map<String, String> buildIdProviderMap( final V1alpha2Xp7VHostSpecMapping m )
    {
        Map<String, String> res = new HashMap<>();
        if ( m.idProviders() == null )
        {
            return res;
        }
        res.put( m.idProviders().defaultIdProvider(), "default" );
        m.idProviders().enabled().forEach( s -> res.put( s, "enabled" ) );
        return res;
    }
}
