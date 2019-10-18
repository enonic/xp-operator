package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class VhostPath
{
    public abstract List<XpDeploymentResourceSpecNode> nodes();

    public abstract String path();

    @Value.Derived
    public Map<String, String> getVhostLabel()
    {
        Map<String, String> res = new HashMap<>();
        nodes().stream().forEach( n -> res.putAll( n.nodeAliasLabel() ) );
        // TODO: 2 nodes with same vHost + path wont work, set as precondition check
        // in Deployment spec
        return res;
    }

    public String getPathResourceName( String appFullName, String vhost )
    {
        String hashString = appFullName + vhost + path();
        return appFullName + hashString.hashCode();
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
        return Objects.equals( VhostPathSet( this ), VhostPathSet( (VhostPath) obj ) );
    }

    private static Set<String> VhostPathSet( VhostPath path )
    {
        Set<String> res = new HashSet<>();
        for ( XpDeploymentResourceSpecNode node : path.nodes() )
        {
            res.add( path.path() + node.alias() );
        }
        return res;
    }
}
