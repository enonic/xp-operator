package com.enonic.cloud.operator.operators.common.queues;

import io.fabric8.kubernetes.api.model.HasMetadata;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

public abstract class NodeGroupChangeAggregator<T extends HasMetadata>
    extends ResourceChangeAggregator<T>
{
    protected boolean matchNodeGroup( String sourceNodeGroup, String targetNodeGroup )
    {
        return targetNodeGroup.equals( sourceNodeGroup ) || targetNodeGroup.equals( cfgStr( "operator.helm.charts.Values.allNodesKey" ) );
    }
}
