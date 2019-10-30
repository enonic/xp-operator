package com.enonic.ec.kubernetes.deployment.diff;

import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.spec.SpecNode;

@Value.Immutable
public abstract class DiffSpecNode
    extends Diff<SpecNode>
{
    @Value.Derived
    public boolean replicasChanged()
    {
        return !equals( SpecNode::replicas );
    }

    @Value.Derived
    public boolean typeChanged()
    {
        return !equals( SpecNode::type );
    }

    @Value.Derived
    public boolean resourcesChanged()
    {
        return !equals( SpecNode::resources );
    }

    @Value.Derived
    public boolean envChanged()
    {
        return !equals( SpecNode::env );
    }

    @Value.Derived
    public boolean configChanged()
    {
        return !equals( SpecNode::config );
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
        Preconditions.checkState( equals( r -> r.resources().disks() ), "cannot change 'resources.disk'" );
    }
}
