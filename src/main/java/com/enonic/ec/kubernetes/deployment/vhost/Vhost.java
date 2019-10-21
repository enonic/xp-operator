package com.enonic.ec.kubernetes.deployment.vhost;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;

@Value.Immutable
public abstract class Vhost
{
    public abstract String host();

    public abstract Optional<XpDeploymentResourceSpecVhostCertificate> certificate();

    public abstract List<VhostPath> vhostPaths();

    public String getVHostResourceName( final XpDeploymentResource resource )
    {
        return resource.spec().defaultResourceName( host().replace( ".", "-" ) );
    }

    @Override
    public String toString()
    {
        return host();
    }
}
