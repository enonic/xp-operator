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
public abstract class VHostPath
    extends Configuration
{
    public abstract List<XpDeploymentResourceSpecNode> nodes();

    public abstract String path();

    @Value.Derived
    public Map<String, String> getServiceSelectorLabels()
    {
        Map<String, String> res = new HashMap<>();
        nodes().forEach( n -> res.putAll( n.nodeAliasLabel() ) );
        return res;
    }

    public Map<String, String> getVHostLabels( VHost vhost )
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
        res.put( cfgStr( "operator.deployment.xp.vHost.label.vHost" ), vhost.host() );
        res.put( cfgStr( "operator.deployment.xp.vHost.label.path" ), pathLabel );
        return res;
    }

    public String getPathResourceName( XpDeploymentResource resource, VHost vhost )
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
        return Objects.equals( VHostPathSet( this ), VHostPathSet( (VHostPath) obj ) );
    }

    private static Set<String> VHostPathSet( VHostPath path )
    {
        Set<String> res = new HashSet<>();
        for ( XpDeploymentResourceSpecNode node : path.nodes() )
        {
            res.add( path.path() + node.alias() );
        }
        return res;
    }
}
