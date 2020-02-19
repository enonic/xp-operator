package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.helpers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.cloud.operator.operators.common.cache.Caches;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

@Value.Immutable
public abstract class MappingBuilder
{
    public static Map<String, List<Mapping>> getNodeMappings( Caches caches, ResourceInfoNamespaced info )
    {
        // Collect all mappings
        List<Mapping> mappings = new LinkedList<>();

        caches.getVHostCache().getByNamespace( info.deploymentInfo().namespaceName() ).
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
        info.deploymentInfo().resource().getSpec().nodeGroups().keySet().forEach( n -> result.put( n, new LinkedList<>() ) );

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

    private static Map<String, String> buildIdProviderMap( final V1alpha2Xp7VHostSpecMapping m )
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
