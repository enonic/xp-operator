package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import java.util.Objects;

import com.enonic.cloud.kubernetes.crd.status.HasStatus;


public class V1alpha2Xp7VHost
    extends HasStatus<V1alpha2Xp7VHostStatusFields, V1alpha2Xp7VHostStatus>
{
    private V1alpha2Xp7VHostSpec spec;

    public V1alpha2Xp7VHostSpec getSpec()
    {
        return spec;
    }

    @SuppressWarnings("unused")
    public void setSpec( V1alpha2Xp7VHostSpec spec )
    {
        this.spec = spec;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha2Xp7VHost) obj ).spec );
    }
}
