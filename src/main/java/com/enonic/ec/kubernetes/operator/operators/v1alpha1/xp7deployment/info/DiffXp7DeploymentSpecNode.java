package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info;

import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;

@Value.Immutable
public abstract class DiffXp7DeploymentSpecNode
    extends Diff<Xp7DeploymentSpecNode>
{
    public abstract String name();

    @Value.Derived
    public boolean replicasChanged()
    {
        return !equals( Xp7DeploymentSpecNode::replicas );
    }

    @Value.Derived
    public boolean typeChanged()
    {
        return !equals( Xp7DeploymentSpecNode::type );
    }

    @Value.Derived
    public boolean resourcesChanged()
    {
        return !equals( Xp7DeploymentSpecNode::resources );
    }

    @Value.Derived
    public boolean envChanged()
    {
        return !equals( Xp7DeploymentSpecNode::env );
    }

    @Value.Derived
    public boolean configChanged()
    {
        return !equals( Xp7DeploymentSpecNode::config );
    }

    @Value.Derived
    public boolean systemConfigChanged()
    {
        return !equals( s -> s.config().entrySet().stream().
            filter( e -> e.getKey().endsWith( ".properties" ) ).
            collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
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
