package com.enonic.ec.kubernetes.operator.crd.config;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.operator.crd.config.spec.Spec;


public class XpConfigResource
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
            return spec.equals( ( (XpConfigResource) obj ).spec );
        }
        return true;
    }
}
