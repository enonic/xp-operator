package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResourceDoneable;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResourceList;

public class Xp7ConfigClient
{
    private final MixedOperation<Xp7ConfigResource, Xp7ConfigResourceList, Xp7ConfigResourceDoneable, Resource<Xp7ConfigResource, Xp7ConfigResourceDoneable>>
        client;

    public Xp7ConfigClient(
        final MixedOperation<Xp7ConfigResource, Xp7ConfigResourceList, Xp7ConfigResourceDoneable, Resource<Xp7ConfigResource, Xp7ConfigResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<Xp7ConfigResource, Xp7ConfigResourceList, Xp7ConfigResourceDoneable, Resource<Xp7ConfigResource, Xp7ConfigResourceDoneable>> client()
    {
        return client;
    }
}
