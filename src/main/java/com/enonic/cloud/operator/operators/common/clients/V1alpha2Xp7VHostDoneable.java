package com.enonic.cloud.operator.operators.common.clients;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7VHostDoneable
    extends CustomResourceDoneable<V1alpha2Xp7VHost>
{
    public V1alpha2Xp7VHostDoneable( V1alpha2Xp7VHost resource, Function<V1alpha2Xp7VHost, V1alpha2Xp7VHost> function )
    {
        super( resource, function );
    }
}