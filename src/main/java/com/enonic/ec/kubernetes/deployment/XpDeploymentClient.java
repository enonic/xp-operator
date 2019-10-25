package com.enonic.ec.kubernetes.deployment;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceDoneable;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceList;

public class XpDeploymentClient
{
    private final MixedOperation<XpDeploymentResource, XpDeploymentResourceList, XpDeploymentResourceDoneable, Resource<XpDeploymentResource, XpDeploymentResourceDoneable>>
        client;

    public XpDeploymentClient(
        final MixedOperation<XpDeploymentResource, XpDeploymentResourceList, XpDeploymentResourceDoneable, Resource<XpDeploymentResource, XpDeploymentResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<XpDeploymentResource, XpDeploymentResourceList, XpDeploymentResourceDoneable, Resource<XpDeploymentResource, XpDeploymentResourceDoneable>> client()
    {
        return client;
    }
}
