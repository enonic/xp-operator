package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

@Value.Immutable
public abstract class ClusterConfig
    extends ClusterConfigurator
{
    @Override
    protected void setClusterConfig( final StringBuilder sb )
    {
        String podHost = "${env.XP_NODE_NAME}." + info().allNodesServiceName();
        sb.append( "cluster.enabled=" ).append( "true" ).append( "\n" );
        sb.append( "node.name=" ).append( "${env.XP_NODE_NAME}" ).append( "\n" );
        sb.append( "discovery.unicast.hosts=" ).append( String.join( ",", createWaitForDnsRecordsList( info() ) ) ).append( "\n" );
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
    protected void setElasticSearchConfig( final StringBuilder sb, Xp7DeploymentSpecNode node )
    {
        sb.append( "node.master=" ).append( node.isMasterNode() ? "true" : "false" ).append( "\n" );
        sb.append( "node.data=" ).append( node.isDataNode() ? "true" : "false" ).append( "\n" );
        sb.append( "cluster.name=" ).append( info().deploymentName() ).append( "\n" );
        sb.append( "http.enabled=" ).append( "false" ).append( "\n" );
        sb.append( "gateway.expected_master_nodes=" ).append( info().minimumMasterNodes() ).append( "\n" );
        sb.append( "gateway.expected_data_nodes=" ).append( info().minimumDataNodes() ).append( "\n" );
        sb.append( "gateway.recover_after_time=" ).append( "5m" ).append( "\n" );
        sb.append( "discovery.zen.minimum_master_nodes=" ).append( info().minimumMasterNodes() ).append( "\n" );
        sb.append( "network.tcp.keep_alive=" ).append( "true" ).append( "\n" );
    }

    @Override
    protected List<String> createWaitForDnsRecordsList( final InfoXp7Deployment info )
    {
        List<String> res = new LinkedList<>();
        for ( Map.Entry<String, Xp7DeploymentSpecNode> node : info.resource().getSpec().nodes().entrySet() )
        {
            if ( node.getValue().isMasterNode() )
            {
                for ( int i = 0; i < node.getValue().replicas(); i++ )
                {
                    res.add( node.getKey() + "-" + i + "." + info.allNodesServiceName() );
                }
            }
        }
        return res;
    }
}
