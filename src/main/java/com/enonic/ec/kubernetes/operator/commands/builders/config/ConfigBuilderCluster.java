package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

@Value.Immutable
public abstract class ConfigBuilderCluster
    implements ConfigBuilder
{
    protected abstract String namespace();

    protected abstract String esDiscoveryService();

    protected abstract String clusterName();

    protected abstract Integer minimumMasterNodes();

    @Override
    public Map<String, String> create( Map<String, String> defaultConfig )
    {
        Map<String, String> config = new HashMap<>( defaultConfig );

        String podDnsRecord = dnsRecord( "${env.XP_NODE_NAME}", esDiscoveryService(), namespace() );
        String discoveryService = dnsRecord( esDiscoveryService(), namespace() );

        StringBuilder clusterCfg = new StringBuilder();
        sbAdd( clusterCfg, "cluster.enabled", "true" );
        sbAdd( clusterCfg, "node.name", "${env.XP_NODE_NAME}" );
        sbAdd( clusterCfg, "discovery.unicast.hosts", discoveryService );
        sbAdd( clusterCfg, "network.host", podDnsRecord );
        sbAdd( clusterCfg, "network.publish.host", podDnsRecord );
        config.put( "com.enonic.xp.cluster.cfg", clusterCfg.toString() );

        StringBuilder elasticCfg = new StringBuilder();
        sbAdd( elasticCfg, "cluster.name", clusterName() );
        sbAdd( elasticCfg, "discovery.zen.minimum_master_nodes", minimumMasterNodes().toString() );
        config.put( "com.enonic.xp.elasticsearch.cfg", elasticCfg.toString() );

        return config;
    }

    private static String dnsRecord( String service, String namespace )
    {
        return String.join( ".", service, namespace, "svc.cluster.local" );
    }

    private static String dnsRecord( String pod, String service, String namespace )
    {
        return dnsRecord( String.join( ".", pod, service ), namespace );
    }

    private void sbAdd( StringBuilder sb, String key, String value )
    {
        sb.append( key ).append( "=" ).append( value ).append( "\n" );
    }
}
