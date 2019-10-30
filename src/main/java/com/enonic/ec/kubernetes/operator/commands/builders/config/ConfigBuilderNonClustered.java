package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecNode;

@Value.Immutable
public abstract class ConfigBuilderNonClustered
    extends ConfigBuilder
{
    @Override
    public Map<String, String> create( SpecNode node )
    {
        Map<String, String> config = new HashMap<>( node.config() );
        Properties clusterCfg = new Properties();
        clusterCfg.put( "cluster.enabled", "false" );
        apply( node, "com.enonic.xp.cluster.cfg", clusterCfg, config );
        return config;
    }
}
