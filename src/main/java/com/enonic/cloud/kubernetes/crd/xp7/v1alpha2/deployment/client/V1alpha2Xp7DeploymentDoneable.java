package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.client;

import io.fabric8.kubernetes.api.builder.Function;

import com.enonic.cloud.kubernetes.crd.status.HasStatusDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatusFields;


@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7DeploymentDoneable
    extends HasStatusDoneable<V1alpha2Xp7DeploymentStatusFields, V1alpha2Xp7DeploymentStatus, V1alpha2Xp7Deployment>
{
    public V1alpha2Xp7DeploymentDoneable( V1alpha2Xp7Deployment resource, Function<V1alpha2Xp7Deployment, V1alpha2Xp7Deployment> function )
    {
        super( resource, function );
    }
}