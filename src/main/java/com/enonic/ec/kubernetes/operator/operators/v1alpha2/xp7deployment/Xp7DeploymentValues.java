package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.ValueBuilder;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

@Value.Immutable
public abstract class Xp7DeploymentValues
    extends ValueBuilder<InfoXp7Deployment>
{
    protected abstract String imageTemplate();

    @Override
    protected void createValues( final Map<String, Object> values )
    {
        InfoXp7Deployment info = info();

        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", info.deploymentName() );
        deployment.put( "allNodeGroupsKey",info.allNodeGroupsKey() );
        deployment.put( "clustered", info.resource().getSpec().isClustered() );
        if ( info.resource().getSpec().isClustered() )
        {
            deployment.put( "discoveryHosts", createDiscoveryHosts( info ) );
            deployment.put( "minimumMasterNodes", info.minimumMasterNodes() );
            deployment.put( "minimumDataNodes", info.minimumDataNodes() );
        }
        deployment.put( "spec", info.resource().getSpec() );

        values.put( "image", String.format( imageTemplate(), info.resource().getSpec().xpVersion() ) );
        values.put( "defaultLabels", info.defaultLabels() );
        values.put( "deployment", deployment );
        //values.put( "ownerReferences", Collections.singletonList( info.ownerReference() ) );
    }

    private String createDiscoveryHosts( InfoXp7Deployment info )
    {
        List<String> res = new LinkedList<>();
        for ( Map.Entry<String, V1alpha2Xp7DeploymentSpecNode> node : info.resource().getSpec().nodeGroups().entrySet() )
        {
            if ( node.getValue().master() )
            {
                for ( int i = 0; i < node.getValue().replicas(); i++ )
                {
                    res.add( node.getKey() + "-" + i + "." + info.allNodeGroupsKey() );
                }
            }
        }
        return String.join( ",", res );
    }
}
