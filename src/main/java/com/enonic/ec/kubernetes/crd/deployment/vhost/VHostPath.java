package com.enonic.ec.kubernetes.crd.deployment.vhost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

@Value.Immutable
public abstract class VHostPath
    extends Configuration
{
    public abstract List<SpecNode> nodes();

    public abstract String path();

    public abstract String pathResourceName();

    public abstract Map<String, String> vHostLabels();

    @Value.Derived
    public Map<String, String> getServiceSelectorLabels()
    {
        Map<String, String> res = new HashMap<>();
        nodes().forEach( n -> res.putAll( n.nodeAliasLabel() ) );
        return res;
    }

    private static Set<String> VHostPathSet( VHostPath path )
    {
        Set<String> res = new HashSet<>();
        for ( SpecNode node : path.nodes() )
        {
            res.add( path.path() + node.alias() );
        }
        return res;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        return Objects.equals( VHostPathSet( this ), VHostPathSet( (VHostPath) obj ) );
    }

    @Override
    public String toString()
    {
        return pathResourceName();
    }
}
