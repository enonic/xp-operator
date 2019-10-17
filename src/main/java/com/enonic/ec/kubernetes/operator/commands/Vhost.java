package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;

@Value.Immutable
public abstract class Vhost
{
    public abstract String host();

    @Nullable
    public abstract XpDeploymentResourceSpecVhostCertificate certificate();

    public abstract List<VhostPath> vhostPaths();

    public Map<String, String> getVhostLabel( XpDeploymentResourceSpecNode node )
    {
        Map<String, String> labels = new HashMap<>();
        for ( VhostPath path : vhostPaths() )
        {
            if ( path.nodes().contains( node ) )
            {
                labels.putAll( path.getVhostLabel(this) );
            }
        }
        return labels;
    }

    public String getVhostResourceName( final String fullAppName )
    {
        return fullAppName + "-" + host().replace( ".", "-" );
    }
}
