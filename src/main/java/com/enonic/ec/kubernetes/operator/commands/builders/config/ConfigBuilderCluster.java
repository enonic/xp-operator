package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.NodeType;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class ConfigBuilderCluster
    extends ConfigBuilder
{
    protected abstract String namespace();

    protected abstract String esDiscoveryService();

    protected abstract String clusterName();

    protected abstract Integer minimumMasterNodes();

    protected abstract Integer minimumDataNodes();

    @Override
    public Map<String, String> create( XpDeploymentResourceSpecNode node )
    {
        Map<String, String> config = new HashMap<>( node.config() );

        String podDnsRecord = dnsRecord( "${env.XP_NODE_NAME}", esDiscoveryService(), namespace() );
        String discoveryService = dnsRecord( esDiscoveryService(), namespace() );

        // Create cluster config
        Properties clusterCfg = new Properties();

        clusterCfg.put( "cluster.enabled", "true" );
        clusterCfg.put( "node.name", "${env.XP_NODE_NAME}" );
        clusterCfg.put( "discovery.unicast.hosts", discoveryService );
        clusterCfg.put( "network.host", podDnsRecord );
        clusterCfg.put( "network.publish.host", podDnsRecord );

        apply( node, "com.enonic.xp.cluster.cfg", clusterCfg, config );

        // Create elastic config
        Properties elasticCfg = new Properties();

        setNodeType( elasticCfg, node.type() );
        elasticCfg.put( "cluster.name", clusterName() );

        elasticCfg.put( "gateway.expected_master_nodes", minimumMasterNodes() );
        elasticCfg.put( "gateway.expected_data_nodes", minimumDataNodes() );
        elasticCfg.put( "discovery.zen.minimum_master_nodes", minimumMasterNodes() );

        elasticCfg.put( "network.tcp.keep_alive", "true" );

        elasticCfg.put( "discovery.zen.fd.ping_timeout", "5s" );
        elasticCfg.put( "discovery.zen.fd.ping_retries", "3" );
        elasticCfg.put( "discovery.zen.fd.ping_interval", "1s" );

        apply( node, "com.enonic.xp.elasticsearch.cfg", elasticCfg, config );

        return config;
    }

    private static void setNodeType( Properties elasticCfg, NodeType type )
    {
        switch ( type )
        {
            case STANDALONE:
                elasticCfg.put( "node.client", "false" );
                elasticCfg.put( "node.master", "true" );
                elasticCfg.put( "node.data", "true" );
                break;
            case COMBINED:
                elasticCfg.put( "node.client", "true" );
                elasticCfg.put( "node.master", "false" );
                elasticCfg.put( "node.data", "true" );
                break;
            case MASTER:
                elasticCfg.put( "node.client", "false" );
                elasticCfg.put( "node.master", "true" );
                elasticCfg.put( "node.data", "false" );
                break;
            case DATA:
                elasticCfg.put( "node.client", "false" );
                elasticCfg.put( "node.master", "false" );
                elasticCfg.put( "node.data", "true" );
                break;
            case FRONT:
                elasticCfg.put( "node.client", "true" );
                elasticCfg.put( "node.master", "false" );
                elasticCfg.put( "node.data", "false" );
                break;
        }
    }

    private static String dnsRecord( String service, String namespace )
    {
        return String.join( ".", service, namespace, "svc.cluster.local" );
    }

    private static String dnsRecord( String pod, String service, String namespace )
    {
        return dnsRecord( String.join( ".", pod, service ), namespace );
    }
}
