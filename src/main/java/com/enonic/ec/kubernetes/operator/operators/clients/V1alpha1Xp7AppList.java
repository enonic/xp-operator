package com.enonic.ec.kubernetes.operator.operators.clients;

import io.fabric8.kubernetes.client.CustomResourceList;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;

@SuppressWarnings("WeakerAccess")
public class V1alpha1Xp7AppList
    extends CustomResourceList<V1alpha1Xp7App>
{
}
