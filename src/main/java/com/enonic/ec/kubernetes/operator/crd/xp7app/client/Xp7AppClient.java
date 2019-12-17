package com.enonic.ec.kubernetes.operator.crd.xp7app.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResourceList;

public class Xp7AppClient
{
    private final MixedOperation<Xp7AppResource, Xp7AppResourceList, Xp7AppResourceDoneable, Resource<Xp7AppResource, Xp7AppResourceDoneable>>
        client;

    public Xp7AppClient(
        final MixedOperation<Xp7AppResource, Xp7AppResourceList, Xp7AppResourceDoneable, Resource<Xp7AppResource, Xp7AppResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<Xp7AppResource, Xp7AppResourceList, Xp7AppResourceDoneable, Resource<Xp7AppResource, Xp7AppResourceDoneable>> client()
    {
        return client;
    }
}
