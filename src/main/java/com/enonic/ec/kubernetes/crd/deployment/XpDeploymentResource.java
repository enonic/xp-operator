package com.enonic.ec.kubernetes.crd.deployment;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.crd.deployment.spec.Spec;

public class XpDeploymentResource
    extends CustomResource
{
    private Spec spec;

    public Spec getSpec()
    {
        return spec;
    }

    public void setSpec( Spec spec )
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
