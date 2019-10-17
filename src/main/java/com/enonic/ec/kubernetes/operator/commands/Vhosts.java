package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

public class Vhosts
    extends LinkedList<Vhost>
{

    public Map<String, String> getVhostLabel( XpDeploymentResourceSpecNode node )
    {
        Map<String, String> labels = new HashMap<>();
        for ( Vhost vhost : this )
        {
            labels.putAll( vhost.getVhostLabel( node ) );
        }
        return labels;
    }
}
