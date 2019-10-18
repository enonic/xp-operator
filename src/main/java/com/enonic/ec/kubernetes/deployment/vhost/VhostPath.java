package com.enonic.ec.kubernetes.deployment.vhost;

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
        return res;
    }

    public String getPathResourceName( String appFullName, String vhost )
    {
        String hashString = appFullName + vhost + path();
        return String.join( "-", appFullName, vhost.replace( ".", "-" ), Integer.toString( Math.abs( path().hashCode() ) ) );
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
        boolean result = Objects.equals( VhostPathSet( this ), VhostPathSet( (VhostPath) obj ) );
        return result;
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
