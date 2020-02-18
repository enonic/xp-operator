package com.enonic.cloud.operator.operators.common.clients;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;

@SuppressWarnings("WeakerAccess")
public class V1alpha1Xp7AppDoneable
    extends CustomResourceDoneable<V1alpha1Xp7App>
{
    public V1alpha1Xp7AppDoneable( V1alpha1Xp7App resource, Function<V1alpha1Xp7App, V1alpha1Xp7App> function )
    {
        super( resource, function );
    }
}