package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResourceDoneable;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResourceList;

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
