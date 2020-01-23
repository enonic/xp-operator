package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResourceDoneable;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResourceList;

public class Xp7VHostClient
{
    private final MixedOperation<Xp7VHostResource, Xp7VHostResourceList, Xp7VHostResourceDoneable, Resource<Xp7VHostResource, Xp7VHostResourceDoneable>>
        client;

    public Xp7VHostClient(
        final MixedOperation<Xp7VHostResource, Xp7VHostResourceList, Xp7VHostResourceDoneable, Resource<Xp7VHostResource, Xp7VHostResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<Xp7VHostResource, Xp7VHostResourceList, Xp7VHostResourceDoneable, Resource<Xp7VHostResource, Xp7VHostResourceDoneable>> client()
    {
        return client;
    }
}
