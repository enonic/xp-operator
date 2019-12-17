package com.enonic.ec.kubernetes.operator.crd.xp7deployment;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class Xp7DeploymentResourceDoneable
    extends CustomResourceDoneable<Xp7DeploymentResource>
{
    public Xp7DeploymentResourceDoneable( Xp7DeploymentResource resource, Function<Xp7DeploymentResource, Xp7DeploymentResource> function )
    {
        super( resource, function );
    }
}