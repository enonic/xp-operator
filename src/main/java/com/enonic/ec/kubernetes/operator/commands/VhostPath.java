package com.enonic.ec.kubernetes.operator.commands;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class VhostPath
{
    public abstract List<XpDeploymentResourceSpecNode> nodes();

    public abstract String path();

    public Map<String, String> getVhostLabel(Vhost vhost)
    {
        String pathString;
        if(path().equals( "/" )) {
            pathString = "root";
        }
        else {
            pathString = path().substring( 1 ).replace( "/", "_" );
        }
        return Map.of("xp.vhost/" + vhost.host(), pathString);
    }

    public String getPathResourceName( String appFullName, String vhost )
    {
        String hashString = appFullName + vhost + path();
        return appFullName + hashString.hashCode();
    }
}
