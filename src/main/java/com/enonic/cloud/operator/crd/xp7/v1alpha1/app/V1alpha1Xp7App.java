package com.enonic.cloud.operator.crd.xp7.v1alpha1.app;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;


public class V1alpha1Xp7App
    extends CustomResource
{
    private V1alpha1Xp7AppSpec spec;

    public V1alpha1Xp7AppSpec getSpec()
    {
        return spec;
    }

    @SuppressWarnings("unused")
    public void setSpec( V1alpha1Xp7AppSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha1Xp7App) obj ).spec );
    }
}
