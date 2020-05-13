package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.client;

import io.fabric8.kubernetes.api.builder.Function;

import com.enonic.cloud.kubernetes.crd.CrdDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7ConfigSpec;


@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7ConfigDoneable
    extends CrdDoneable<V1alpha2Xp7Config>
{
    public V1alpha2Xp7ConfigDoneable( final V1alpha2Xp7Config resource, final Function<V1alpha2Xp7Config, V1alpha2Xp7Config> function )
    {
        super( resource, function );
    }

    public V1alpha2Xp7ConfigDoneable withSpec( V1alpha2Xp7ConfigSpec spec )
    {
        resource.setSpec( spec );
        return this;
    }
}