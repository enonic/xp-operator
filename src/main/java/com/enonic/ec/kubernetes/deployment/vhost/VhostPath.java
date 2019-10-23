package com.enonic.ec.kubernetes.deployment.vhost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class VhostPath
    extends Configuration
{
    public abstract List<XpDeploymentResourceSpecNode> nodes();

    public abstract String path();

    @Value.Derived
    public Map<String, String> getServiceSelectorLabels()
    {
        Map<String, String> res = new HashMap<>();
        nodes().stream().forEach( n -> res.putAll( n.nodeAliasLabel() ) );
        return res;
    }

    public Map<String, String> getVhostLabels( Vhost vhost )
    {
        String pathLabel = path().replace( "/", "_" );
        if ( pathLabel.equals( "_" ) )
        {
            pathLabel = "root";
        }
        else
        {
            pathLabel = pathLabel.substring( 1 );
        }
        Map<String, String> res = new HashMap<>();
        res.put( cfgStr( "operator.deployment.xp.vhost.label.vHost" ), vhost.host() );
        res.put( cfgStr( "operator.deployment.xp.vhost.label.path" ), pathLabel );
        return res;
    }

    public String getPathResourceName( XpDeploymentResource resource, Vhost vhost )
    {
        // TODO: Fix hash
        return resource.getSpec().defaultResourceNameWithPostFix( vhost.host().replace( ".", "-" ),
                                                                  Integer.toString( Math.abs( path().hashCode() ) ) );
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
