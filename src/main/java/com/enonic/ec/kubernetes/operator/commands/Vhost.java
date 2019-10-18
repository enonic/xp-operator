package com.enonic.ec.kubernetes.operator.commands;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;

@Value.Immutable
public abstract class Vhost
{
    public abstract String host();

    @Nullable
    public abstract XpDeploymentResourceSpecVhostCertificate certificate();

    public abstract List<VhostPath> vhostPaths();

    public String getVhostResourceName( final String fullAppName )
    {
        return fullAppName + "-" + host().replace( ".", "-" );
    }
}
