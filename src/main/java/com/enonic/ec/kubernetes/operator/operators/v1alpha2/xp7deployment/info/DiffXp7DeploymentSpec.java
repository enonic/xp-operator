package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;

@Value.Immutable
public abstract class DiffXp7DeploymentSpec
    extends Diff<V1alpha2Xp7DeploymentSpec>
{
    @Value.Derived
    public boolean versionChanged()
    {
        return !equals( V1alpha2Xp7DeploymentSpec::xpVersion );
    }

    @Value.Derived
    public boolean enabledChanged()
    {
        return !equals( V1alpha2Xp7DeploymentSpec::enabled );
    }

    @Value.Derived
    public List<DiffXp7DeploymentSpecNode> nodesChanged()
    {
        Map<String, V1alpha2Xp7DeploymentSpecNode> oldNodes = oldValue().map( V1alpha2Xp7DeploymentSpec::nodeGroups ).orElse( Collections.emptyMap() );
        Map<String, V1alpha2Xp7DeploymentSpecNode> newNodes = newValue().map( V1alpha2Xp7DeploymentSpec::nodeGroups ).orElse( Collections.emptyMap() );
        return mergeMaps( oldNodes, newNodes, ( s, o, n ) -> ImmutableDiffXp7DeploymentSpecNode.builder().
            name( s ).
            oldValue( o ).
            newValue( n ).
            build() );
    }

    @Value.Check
    protected void check()
    {
        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( V1alpha2Xp7DeploymentSpec::nodesSharedDisks ), "Field 'spec.nodesSharedDisk' cannot be changed" );
    }

}
