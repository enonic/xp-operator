package com.enonic.ec.kubernetes.crd.deployment.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.crd.deployment.spec.Spec;
import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

@Value.Immutable
public abstract class DiffSpec
    extends Diff<Spec>
{
    @Value.Derived
    public boolean versionChanged()
    {
        return !equals( Spec::xpVersion );
    }

    @Value.Derived
    public boolean enabledChanged()
    {
        return !equals( Spec::enabled );
    }

    @Value.Derived
    public List<DiffSpecNode> nodesChanged()
    {
        Map<String, SpecNode> oldNodes = new HashMap<>();
        Map<String, SpecNode> newNodes = new HashMap<>();

        oldValue().ifPresent( s -> s.nodes().forEach( n -> oldNodes.put( n.alias(), n ) ) );
        newValue().ifPresent( s -> s.nodes().forEach( n -> newNodes.put( n.alias(), n ) ) );

        return mergeMaps( oldNodes, newNodes, ( o, n ) -> ImmutableDiffSpecNode.builder().
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
        Preconditions.checkState( equals( Spec::sharedDisk ), "cannot change 'sharedDisks'" );
    }

}
