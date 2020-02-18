package com.enonic.cloud.operator.operators.common.clients;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;

@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7ConfigDoneable
    extends CustomResourceDoneable<V1alpha2Xp7Config>
{
    public V1alpha2Xp7ConfigDoneable( V1alpha2Xp7Config resource, Function<V1alpha2Xp7Config, V1alpha2Xp7Config> function )
    {
        super( resource, function );
    }
}