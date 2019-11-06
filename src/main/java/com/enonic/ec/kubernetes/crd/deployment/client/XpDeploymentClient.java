package com.enonic.ec.kubernetes.crd.deployment.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResourceDoneable;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResourceList;

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
