package com.enonic.ec.kubernetes.common.crd.XpDeployment;

import io.fabric8.kubernetes.client.CustomResource;

public class XpDeploymentResource
    extends CustomResource
{

    private XpDeploymentResourceSpec spec;

    public XpDeploymentResourceSpec getSpec()
    {
        return spec;
    }

    public void setSpec( XpDeploymentResourceSpec spec )
    {
        this.spec = spec;
    }

}
