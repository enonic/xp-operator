package com.enonic.ec.kubernetes.crd.deployment;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class XpDeploymentResourceDoneable
    extends CustomResourceDoneable<XpDeploymentResource>
{
    public XpDeploymentResourceDoneable( XpDeploymentResource resource, Function<XpDeploymentResource, XpDeploymentResource> function )
    {
        super( resource, function );
    }
}