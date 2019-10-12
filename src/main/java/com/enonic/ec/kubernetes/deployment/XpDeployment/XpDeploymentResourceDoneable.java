package com.enonic.ec.kubernetes.deployment.XpDeployment;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class XpDeploymentResourceDoneable
    extends CustomResourceDoneable<XpDeploymentResource>
{
    public XpDeploymentResourceDoneable( XpDeploymentResource resource, Function<XpDeploymentResource, XpDeploymentResource> function )
    {
        super( resource, function );
    }
}