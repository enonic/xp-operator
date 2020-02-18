package com.enonic.cloud.operator.crd.xp7.v1alpha2.config;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;


public class V1alpha2Xp7Config
    extends CustomResource
{
    private V1alpha2Xp7ConfigSpec spec;

    public V1alpha2Xp7Config()
    {
        setKind( "Xp7Config" );
    }

    public V1alpha2Xp7ConfigSpec getSpec()
    {
        return spec;
    }

    public void setSpec( V1alpha2Xp7ConfigSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha2Xp7Config) obj ).spec );
    }
}
