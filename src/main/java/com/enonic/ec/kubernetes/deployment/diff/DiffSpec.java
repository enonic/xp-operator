package com.enonic.ec.kubernetes.deployment.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.Spec;
import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecNode;

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

    @Value.Derived
    public List<DiffVHost> vHostsChanged()
    {
        Map<String, VHost> oldVHosts = new HashMap<>();
        Map<String, VHost> newVHosts = new HashMap<>();

        oldValue().ifPresent( s -> s.vHosts().forEach( h -> oldVHosts.put( h.host(), h ) ) );
        newValue().ifPresent( s -> s.vHosts().forEach( h -> newVHosts.put( h.host(), h ) ) );

        return mergeMaps( oldVHosts, newVHosts, ( o, n ) -> ImmutableDiffVHost.builder().
            oldValue( o ).
            newValue( n ).
            build() );
    }

    @Value.Check
    protected void check()
    {
        if ( !shouldDoSomeChange() )
        {
            return;
        }
        Preconditions.checkState( !equals( Spec::cloud ), "cannot change 'cloud'" );
        Preconditions.checkState( !equals( Spec::project ), "cannot change 'project'" );
        Preconditions.checkState( !equals( Spec::app ), "cannot change 'app'" );
        Preconditions.checkState( !equals( Spec::name ), "cannot change 'name'" );
        Preconditions.checkState( !equals( Spec::sharedDisks ), "cannot change 'sharedDisks'" );
    }

}
