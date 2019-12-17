package com.enonic.ec.kubernetes.operator.crd.xp7deployment.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResourceList;

public class Xp7DeploymentClient
{
    private final MixedOperation<Xp7DeploymentResource, Xp7DeploymentResourceList, Xp7DeploymentResourceDoneable, Resource<Xp7DeploymentResource, Xp7DeploymentResourceDoneable>>
        client;

    public Xp7DeploymentClient(
        final MixedOperation<Xp7DeploymentResource, Xp7DeploymentResourceList, Xp7DeploymentResourceDoneable, Resource<Xp7DeploymentResource, Xp7DeploymentResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<Xp7DeploymentResource, Xp7DeploymentResourceList, Xp7DeploymentResourceDoneable, Resource<Xp7DeploymentResource, Xp7DeploymentResourceDoneable>> client()
    {
        return client;
    }
}
