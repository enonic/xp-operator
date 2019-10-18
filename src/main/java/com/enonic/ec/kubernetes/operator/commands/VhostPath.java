package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class VhostPath
{
    public abstract List<XpDeploymentResourceSpecNode> nodes();

    public abstract String path();

    public Map<String, String> getVhostLabel()
    {
        Map<String, String> res = new HashMap<>();
        nodes().stream().forEach( n -> res.putAll( n.nodeAliasLabel() ) );
        return res;
    }

    public String getPathResourceName( String appFullName, String vhost )
    {
        String hashString = appFullName + vhost + path();
        return appFullName + hashString.hashCode();
    }
}
