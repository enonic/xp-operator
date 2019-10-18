package com.enonic.ec.kubernetes.deployment.vhost;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;

@Value.Immutable
public abstract class Vhost
{
    public abstract String host();

    @Nullable
    public abstract XpDeploymentResourceSpecVhostCertificate certificate();

    public abstract List<VhostPath> vhostPaths();

    public String getVHostResourceName( final XpDeploymentResource resource )
    {
        return resource.getSpec().defaultResourceName( host().replace( ".", "-" ) );
    }
}
