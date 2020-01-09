package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpec;


public class Xp7VHostResource
    extends CustomResource
{
    private Xp7VHostSpec spec;

    public Xp7VHostSpec getSpec()
    {
        return spec;
    }

    public void setSpec( Xp7VHostSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (Xp7VHostResource) obj ).spec );
    }
}
