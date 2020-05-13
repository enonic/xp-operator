package com.enonic.cloud.kubernetes.crd.status;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

public class HasStatus<F, S extends CrdStatus<F>>
    extends CustomResource
{
    @SuppressWarnings("WeakerAccess")
    protected S status;

    public S getStatus()
    {
        return status;
    }

    public void setStatus( final S status )
    {
        this.status = status;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( status, ( (V1alpha2Xp7Deployment) obj ).status );
    }
}
