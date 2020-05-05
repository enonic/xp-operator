package com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;


public class V1alpha2Xp7VHost
    extends CustomResource
{
    private V1alpha2Xp7VHostSpec spec;

    private V1alpha2Xp7VHostStatus status;

    public V1alpha2Xp7VHostSpec getSpec()
    {
        return spec;
    }

    @SuppressWarnings("unused")
    public void setSpec( V1alpha2Xp7VHostSpec spec )
    {
        this.spec = spec;
    }

    public V1alpha2Xp7VHostStatus getStatus()
    {
        return status;
    }

    public void setStatus( final V1alpha2Xp7VHostStatus status )
    {
        this.status = status;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha2Xp7VHost) obj ).spec );
    }
}
