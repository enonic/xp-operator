package com.enonic.ec.kubernetes.operator.crd.xp7vhost.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResourceList;

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
