package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;

@Value.Immutable
public abstract class DiffXp7DeploymentSpec
    extends Diff<Xp7DeploymentSpec>
{
    @Value.Derived
    public boolean versionChanged()
    {
        return !equals( Xp7DeploymentSpec::xpVersion );
    }

    @Value.Derived
    public boolean enabledChanged()
    {
        return !equals( Xp7DeploymentSpec::enabled );
    }

    @Value.Derived
    public boolean nodeSharedConfigChanged()
    {
        return !equals( Xp7DeploymentSpec::nodesSharedConfig );
    }

    @Value.Derived
    public List<DiffXp7DeploymentSpecNode> nodesChanged()
    {
        Map<String, Xp7DeploymentSpecNode> oldNodes = oldValue().map( Xp7DeploymentSpec::nodes ).orElse( Collections.emptyMap() );
        Map<String, Xp7DeploymentSpecNode> newNodes = newValue().map( Xp7DeploymentSpec::nodes ).orElse( Collections.emptyMap() );
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

        Preconditions.checkState( equals( Xp7DeploymentSpec::nodesSharedDisk ), "Field 'spec.nodesSharedDisk' cannot be changed" );
    }

}
