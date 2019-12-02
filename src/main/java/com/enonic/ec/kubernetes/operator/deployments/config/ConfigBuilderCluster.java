package com.enonic.ec.kubernetes.operator.deployments.config;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

@Value.Immutable
public abstract class ConfigBuilderCluster
    extends ConfigBuilderNode
{
    protected abstract String namespace();

    protected abstract String serviceName();

    protected abstract String clusterName();

    protected abstract Integer minimumMasterNodes();

    protected abstract Integer minimumDataNodes();

    protected abstract List<String> discoveryHosts();

    @Override
    public Map<String, String> create( SpecNode node )
    {
        Map<String, String> newConfig = overrideWithValues( node.config() );

        String podHost = "${env.XP_NODE_NAME}." + serviceName();

        // Create cluster config
        setIfNotSet( newConfig, "com.enonic.xp.cluster.cfg", setFunc -> {
            setFunc.apply( "cluster.enabled", "true" );
            setFunc.apply( "node.name", "${env.XP_NODE_NAME}" );
            setFunc.apply( "discovery.unicast.hosts", String.join( ",", discoveryHosts() ) );

            // If Linkerd is enabled, bind to localhost for envy proxy
            if ( cfgBool( "operator.extensions.linkerd.enabled" ) )
            {
                setFunc.apply( "network.host", "127.0.0.1" );
            }
            else
            {
                setFunc.apply( "network.host", podHost );
            }

            setFunc.apply( "network.publish.host", podHost );
        } );

        // Create elastic config
        setIfNotSet( newConfig, "com.enonic.xp.elasticsearch.cfg", setFunc -> {
            setFunc.apply( "node.master", node.isMasterNode() ? "true" : "false" );
            setFunc.apply( "node.data", node.isDataNode() ? "true" : "false" );
            setFunc.apply( "cluster.name", clusterName() );

            //elasticCfg.put( "http.enabled", node.isMasterNode() || node.isDataNode() ? "true" : "false" );
            setFunc.apply( "http.enabled", "true" ); // TODO: Use alive app for health checks

            setFunc.apply( "gateway.expected_master_nodes", minimumMasterNodes() );
            setFunc.apply( "gateway.expected_data_nodes", minimumDataNodes() );
            setFunc.apply( "gateway.recover_after_time", "5m" );
            setFunc.apply( "discovery.zen.minimum_master_nodes", minimumMasterNodes() );

            setFunc.apply( "network.tcp.keep_alive", "true" );
        } );

        return newConfig;
    }
}
