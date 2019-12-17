package com.enonic.ec.kubernetes.operator.crd.xp7config;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.operator.crd.xp7config.spec.Xp7ConfigSpec;


public class Xp7ConfigResource
    extends CustomResource
{
    private Xp7ConfigSpec spec;

    public Xp7ConfigSpec getSpec()
    {
        return spec;
    }

    public void setSpec( Xp7ConfigSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (Xp7ConfigResource) obj ).spec );
    }
}
