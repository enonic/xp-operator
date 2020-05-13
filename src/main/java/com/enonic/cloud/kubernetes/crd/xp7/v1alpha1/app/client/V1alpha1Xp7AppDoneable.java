package com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.client;

import java.util.List;

import io.fabric8.kubernetes.api.builder.Function;

import com.enonic.cloud.kubernetes.crd.status.HasStatusDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7AppStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7AppStatusFields;


@SuppressWarnings("WeakerAccess")
public class V1alpha1Xp7AppDoneable
    extends HasStatusDoneable<V1alpha1Xp7AppStatusFields, V1alpha1Xp7AppStatus, V1alpha1Xp7App>
{
    public V1alpha1Xp7AppDoneable( V1alpha1Xp7App resource, Function<V1alpha1Xp7App, V1alpha1Xp7App> function )
    {
        super( resource, function );
    }

    public V1alpha1Xp7AppDoneable withFinalizers( List<String> finalizers )
    {
        resource.getMetadata().setFinalizers( finalizers );
        return this;
    }
}