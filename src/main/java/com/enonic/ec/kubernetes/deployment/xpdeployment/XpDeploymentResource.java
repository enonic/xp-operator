package com.enonic.ec.kubernetes.deployment.xpdeployment;

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

    @Override
    public boolean equals( final Object obj )
    {
        if ( super.equals( obj ) || spec != null )
        {
            return spec.equals( ( (XpDeploymentResource) obj ).spec );
        }
        return true;
    }
}
