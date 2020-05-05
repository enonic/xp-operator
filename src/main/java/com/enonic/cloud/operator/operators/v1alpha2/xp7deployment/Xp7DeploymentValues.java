package com.enonic.cloud.operator.operators.v1alpha2.xp7deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;
import com.enonic.cloud.operator.helm.commands.ValueBuilder;
import com.enonic.cloud.operator.operators.common.DefaultValues;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

@Value.Immutable
public abstract class Xp7DeploymentValues
    extends DefaultValues
    implements ValueBuilder
{
    protected abstract Map<String, Object> baseValues();

    protected abstract InfoXp7Deployment info();

    private Map<String, Object> values( V1alpha2Xp7Deployment resource )
    {
        Map<String, Object> values = new HashMap<>( baseValues() );

        boolean isClustered = isClustered( resource );

        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", info().name() );
        deployment.put( "namespace", info().namespace() );
        deployment.put( "clustered", isClustered );
        if ( isClustered )
        {
            deployment.put( "minimumMasterNodes", minimumMasterNodes( resource ) );
            deployment.put( "minimumDataNodes", minimumDataNodes( resource ) );
        }
        deployment.put( "spec", resource.getSpec() );

        values.put( "defaultLabels", defaultLabels( resource ) );
        values.put( "deployment", deployment );
        //values.put( "ownerReferences", Collections.singletonList( info.ownerReference() ) );
        return values;
    }

    private boolean isClustered( V1alpha2Xp7Deployment resource )
    {
        return resource.getSpec().nodeGroups().values().stream().mapToInt( V1alpha2Xp7DeploymentSpecNode::replicas ).sum() > 1;
    }

    private String allNodeGroupsKey()
    {
        return cfgStr( "operator.helm.charts.Values.allNodesKey" );
    }

    private Integer minimumMasterNodes( V1alpha2Xp7Deployment resource )
    {
        return defaultMinimumAvailable( resource.getSpec().totalMasterNodes() );
    }

    private Integer minimumDataNodes( V1alpha2Xp7Deployment resource )
    {
        return defaultMinimumAvailable( resource.getSpec().totalDataNodes() );
    }

    private Integer defaultMinimumAvailable( int total )
    {
        if ( total < 2 )
        {
            return 0;
        }
        return ( total / 2 ) + 1;
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
