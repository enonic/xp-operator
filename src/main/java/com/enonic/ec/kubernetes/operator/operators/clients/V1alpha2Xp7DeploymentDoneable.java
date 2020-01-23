package com.enonic.ec.kubernetes.operator.operators.clients;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7DeploymentDoneable
    extends CustomResourceDoneable<V1alpha2Xp7Deployment>
{
    public V1alpha2Xp7DeploymentDoneable( V1alpha2Xp7Deployment resource, Function<V1alpha2Xp7Deployment, V1alpha2Xp7Deployment> function )
    {
        super( resource, function );
    }
}