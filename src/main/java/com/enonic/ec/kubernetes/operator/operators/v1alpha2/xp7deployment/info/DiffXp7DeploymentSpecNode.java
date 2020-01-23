package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;

@Value.Immutable
public abstract class DiffXp7DeploymentSpecNode
    extends Diff<V1alpha2Xp7DeploymentSpecNode>
{
    public abstract String name();

    @Value.Derived
    public boolean replicasChanged()
    {
        return !equals( V1alpha2Xp7DeploymentSpecNode::replicas );
    }

    @Value.Derived
    public boolean typeChanged()
    {
        return !equals( V1alpha2Xp7DeploymentSpecNode::master ) || !equals( V1alpha2Xp7DeploymentSpecNode::data );
    }

    @Value.Derived
    public boolean resourcesChanged()
    {
        return !equals( V1alpha2Xp7DeploymentSpecNode::resources );
    }

    @Value.Derived
    public boolean envChanged()
    {
        return !equals( V1alpha2Xp7DeploymentSpecNode::env );
    }

    @Value.Check
    protected void check()
    {
        if ( !shouldModify() )
        {
            return;
        }
        Preconditions.checkState( equals( r -> r.resources().disks() ), "Field 'spec.nodes.resources.disks' cannot be changed" );
    }
}
