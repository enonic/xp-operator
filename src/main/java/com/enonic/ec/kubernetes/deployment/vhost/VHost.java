package com.enonic.ec.kubernetes.deployment.vhost;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVHostCertificate;

@Value.Immutable
public abstract class VHost
{
    public abstract String host();

    public abstract Optional<XpDeploymentResourceSpecVHostCertificate> certificate();

    public abstract List<VHostPath> vHostPaths();

    public String getVHostResourceName( final XpDeploymentResource resource )
    {
        return resource.getSpec().defaultResourceNameWithPostFix( host().replace( ".", "-" ) );
    }

    @Override
    public String toString()
    {
        return host();
    }
}
