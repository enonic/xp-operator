package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.helm.BaseValues;
import com.enonic.ec.kubernetes.operator.helm.commands.ValueBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;

@Value.Immutable
public abstract class Xp7DeploymentValues
    implements ValueBuilder
{
    protected abstract BaseValues baseValues();

    protected abstract String imageTemplate();

    protected abstract InfoXp7Deployment info();

    private Map<String, Object> values( V1alpha2Xp7Deployment resource )
    {
        Map<String, Object> values = new HashMap<>( baseValues() );

        boolean isClustered = isClustered( resource );

        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", info().deploymentName() );
        deployment.put( "allNodeGroupsKey", allNodeGroupsKey() );
        deployment.put( "clustered", isClustered );
        if ( isClustered )
        {
            deployment.put( "discoveryHosts", createDiscoveryHosts( resource ) );
            deployment.put( "minimumMasterNodes", minimumMasterNodes( resource ) );
            deployment.put( "minimumDataNodes", minimumDataNodes( resource ) );
        }
        deployment.put( "spec", resource.getSpec() );

        values.put( "image", String.format( imageTemplate(), resource.getSpec().xpVersion() ) );
        values.put( "defaultLabels", defaultLabels( resource ) );
        values.put( "deployment", deployment );
        //values.put( "ownerReferences", Collections.singletonList( info.ownerReference() ) );
        return values;
    }

    private String createDiscoveryHosts( V1alpha2Xp7Deployment resource )
    {
        List<String> res = new LinkedList<>();
        for ( Map.Entry<String, V1alpha2Xp7DeploymentSpecNode> node : resource.getSpec().nodeGroups().entrySet() )
        {
            if ( node.getValue().master() )
            {
                for ( int i = 0; i < node.getValue().replicas(); i++ )
                {
                    res.add( node.getKey() + "-" + i + "." + allNodeGroupsKey() );
                }
            }
        }
        return String.join( ",", res );
    }

    public boolean isClustered( V1alpha2Xp7Deployment resource )
    {
        return resource.getSpec().nodeGroups().values().stream().mapToInt( V1alpha2Xp7DeploymentSpecNode::replicas ).sum() > 1;
    }

    private String allNodeGroupsKey()
    {
        return cfgStr( "operator.deployment.xp.allNodesKey" );
    }

    private Map<String, String> defaultLabels( V1alpha2Xp7Deployment resource )
    {
        return resource.getMetadata().getLabels();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Integer minimumMasterNodes( V1alpha2Xp7Deployment resource )
    {
        return defaultMinimumAvailable( resource, resource.getSpec().nodeGroups().values().stream().filter(
            V1alpha2Xp7DeploymentSpecNode::master ).findAny().get() );
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Integer minimumDataNodes( V1alpha2Xp7Deployment resource )
    {
        return defaultMinimumAvailable( resource, resource.getSpec().nodeGroups().values().stream().filter(
            V1alpha2Xp7DeploymentSpecNode::data ).findAny().get() );
    }

    private Integer defaultMinimumAvailable( V1alpha2Xp7Deployment resource, V1alpha2Xp7DeploymentSpecNode node )
    {
        if ( !isClustered( resource ) )
        {
            return 0;
        }
        return ( node.replicas() / 2 ) + 1;
    }

    @SuppressWarnings("unused")
    @Value.Derived
    @Override
    public Optional<Object> buildOldValues()
    {
        if ( info().oldResource().isEmpty() )
        {
            return Optional.empty();
        }
        return Optional.of( values( info().oldResource().get() ) );
    }

    @SuppressWarnings("unused")
    @Value.Derived
    @Override
    public Optional<Object> buildNewValues()
    {
        if ( info().newResource().isEmpty() )
        {
            return Optional.empty();
        }
        return Optional.of( values( info().newResource().get() ) );
    }
}
