package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.client;

import io.fabric8.kubernetes.api.builder.Function;

import com.enonic.cloud.kubernetes.crd.status.HasStatusDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatusFields;


@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7VHostDoneable
    extends HasStatusDoneable<V1alpha2Xp7VHostStatusFields, V1alpha2Xp7VHostStatus, V1alpha2Xp7VHost>
{
    public V1alpha2Xp7VHostDoneable( V1alpha2Xp7VHost resource, Function<V1alpha2Xp7VHost, V1alpha2Xp7VHost> function )
    {
        super( resource, function );
    }
}