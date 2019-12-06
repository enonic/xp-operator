package com.enonic.ec.kubernetes.operator.crd.deployment.diff;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.common.Validator;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.Spec;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;

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
    public boolean nodeSharedConfigChanged()
    {
        return !equals( Spec::nodesSharedConfig );
    }

    @Value.Derived
    public List<DiffSpecNode> nodesChanged()
    {
        Map<String, SpecNode> oldNodes = oldValue().map( Spec::nodes ).orElse( Collections.emptyMap() );
        Map<String, SpecNode> newNodes = newValue().map( Spec::nodes ).orElse( Collections.emptyMap() );
        return mergeMaps( oldNodes, newNodes, ( s, o, n ) -> ImmutableDiffSpecNode.builder().
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
        newValue().get().nodes().keySet().forEach( k -> {
            Validator.dnsName( "nodeId", k );
        } );

        Preconditions.checkState( equals( Spec::nodesSharedDisk ), "Field 'spec.nodesSharedDisk' cannot be changed" );
    }

}
