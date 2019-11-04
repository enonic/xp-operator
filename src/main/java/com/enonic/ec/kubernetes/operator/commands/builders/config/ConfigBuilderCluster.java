package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.spec.SpecNode;

@Value.Immutable
public abstract class ConfigBuilderCluster
    extends ConfigBuilder
{
    protected abstract String namespace();

    protected abstract String serviceName();

    protected abstract String clusterName();

    protected abstract Integer minimumMasterNodes();

    protected abstract Integer minimumDataNodes();

    protected abstract List<String> discoveryHosts();

    @Override
    public Map<String, String> create( String nodeResourceName, SpecNode node )
    {
        Map<String, String> config = new HashMap<>( node.config() );

        String podHost = "${env.XP_NODE_NAME}." + serviceName();

        // Create cluster config
        Properties clusterCfg = new Properties();
        clusterCfg.put( "cluster.enabled", "true" );
        clusterCfg.put( "node.name", "${env.XP_NODE_NAME}" );
        clusterCfg.put( "discovery.unicast.hosts", String.join( ",", discoveryHosts() ) );

        // If Linkerd is enabled, bind to localhost for envy proxy
        if ( cfgBool( "operator.extensions.linkerd.enabled" ) )
        {
            clusterCfg.put( "network.host", "127.0.0.1" );
        }
        else
        {
            clusterCfg.put( "network.host", podHost );
        }

        clusterCfg.put( "network.publish.host", podHost );

        apply( node, "com.enonic.xp.cluster.cfg", clusterCfg, config );

        // Create elastic config
        Properties elasticCfg = new Properties();

        setNodeType( elasticCfg, node );
        elasticCfg.put( "cluster.name", clusterName() );
        elasticCfg.put( "http.enabled", "true" ); // For health checks

        elasticCfg.put( "gateway.expected_master_nodes", minimumMasterNodes() );
        elasticCfg.put( "gateway.expected_data_nodes", minimumDataNodes() );
        elasticCfg.put( "discovery.zen.minimum_master_nodes", minimumMasterNodes() );

        elasticCfg.put( "network.tcp.keep_alive", "true" );

//        elasticCfg.put( "discovery.zen.fd.ping_timeout", "5s" );
//        elasticCfg.put( "discovery.zen.fd.ping_retries", "3" );
//        elasticCfg.put( "discovery.zen.fd.ping_interval", "1s" );

        apply( node, "com.enonic.xp.elasticsearch.cfg", elasticCfg, config );

        return config;
    }

    private static void setNodeType( Properties elasticCfg, SpecNode node )
    {
        if ( node.isMasterNode() )
        {
            elasticCfg.put( "node.master", "true" );
        }
        if ( node.isDataNode() )
        {
            elasticCfg.put( "node.data", "true" );
        }
        if ( node.isOnlyFrontend() )
        {
            elasticCfg.put( "node.client", "true" );
            elasticCfg.put( "node.master", "false" );
            elasticCfg.put( "node.data", "false" );
        }
    }
}
