package com.enonic.cloud.operator.crd.xp7.v1alpha1.config;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;


public class V1alpha1Xp7Config
    extends CustomResource
{
    private V1alpha1Xp7ConfigSpec spec;

    @SuppressWarnings("unused")
    public V1alpha1Xp7ConfigSpec getSpec()
    {
        return spec;
    }

    @SuppressWarnings("unused")
    public void setSpec( V1alpha1Xp7ConfigSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha1Xp7Config) obj ).spec );
    }
}
