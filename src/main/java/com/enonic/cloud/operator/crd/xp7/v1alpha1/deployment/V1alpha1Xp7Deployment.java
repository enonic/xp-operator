package com.enonic.cloud.operator.crd.xp7.v1alpha1.deployment;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;

public class V1alpha1Xp7Deployment
    extends CustomResource
{
    private V1alpha1Xp7DeploymentSpec spec;

    @SuppressWarnings("unused")
    public V1alpha1Xp7DeploymentSpec getSpec()
    {
        return spec;
    }

    @SuppressWarnings("unused")
    public void setSpec( V1alpha1Xp7DeploymentSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha1Xp7Deployment) obj ).spec );
    }
}
