package com.enonic.ec.kubernetes.operator.commands.deployments.config;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;

@Value.Immutable
public abstract class ClusterConfig
    extends ClusterConfigurator
{
    protected abstract String serviceName();

    protected abstract List<String> discoveryHosts();

    protected abstract String clusterName();

    protected abstract Integer minimumMasterNodes();

    protected abstract Integer minimumDataNodes();

    @Override
    protected void setClusterConfig( final StringBuilder sb )
    {
        String podHost = "${env.XP_NODE_NAME}." + serviceName();
        sb.append( "cluster.enabled=" ).append( "true" ).append( "\n" );
        sb.append( "node.name=" ).append( "${env.XP_NODE_NAME}" ).append( "\n" );
        sb.append( "discovery.unicast.hosts=" ).append( String.join( ",", discoveryHosts() ) ).append( "\n" );
        sb.append( "network.publish.host=" ).append( podHost ).append( "\n" );
        // If Linkerd is enabled, bind to localhost for envy proxy
        if ( cfgBool( "operator.extensions.linkerd.enabled" ) )
        {
            sb.append( "network.host=" ).append( "127.0.0.1" ).append( "\n" );
        }
        else
        {
            sb.append( "network.host=" ).append( podHost ).append( "\n" );
        }
    }

    @Override
    protected void setElasticSearchConfig( final StringBuilder sb, SpecNode node )
    {
        sb.append( "node.master=" ).append( node.isMasterNode() ? "true" : "false" ).append( "\n" );
        sb.append( "node.data=" ).append( node.isDataNode() ? "true" : "false" ).append( "\n" );
        sb.append( "cluster.name=" ).append( clusterName() ).append( "\n" );
        sb.append( "http.enabled=" ).append( "false" ).append( "\n" );
        sb.append( "gateway.expected_master_nodes=" ).append( minimumMasterNodes() ).append( "\n" );
        sb.append( "gateway.expected_data_nodes=" ).append( minimumDataNodes() ).append( "\n" );
        sb.append( "gateway.recover_after_time=" ).append( "5m" ).append( "\n" );
        sb.append( "discovery.zen.minimum_master_nodes=" ).append( minimumMasterNodes() ).append( "\n" );
        sb.append( "network.tcp.keep_alive=" ).append( "true" ).append( "\n" );
    }
}
