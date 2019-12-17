package com.enonic.ec.kubernetes.operator.crd.xp7app;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.operator.crd.xp7app.spec.Xp7AppSpec;


public class Xp7AppResource
    extends CustomResource
{
    private Xp7AppSpec spec;

    public Xp7AppSpec getSpec()
    {
        return spec;
    }

    public void setSpec( Xp7AppSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (Xp7AppResource) obj ).spec );
    }
}
