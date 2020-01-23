package com.enonic.ec.kubernetes.operator.operators.clients;

import io.fabric8.kubernetes.client.CustomResourceList;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

@SuppressWarnings("WeakerAccess")
public class V1alpha2Xp7DeploymentList
    extends CustomResourceList<V1alpha2Xp7Deployment>
{
}
