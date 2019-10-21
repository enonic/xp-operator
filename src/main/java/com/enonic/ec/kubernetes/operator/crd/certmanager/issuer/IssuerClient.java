package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class IssuerClient
{
    private final MixedOperation<IssuerResource, IssuerResourceList, IssuerResourceDoneable, Resource<IssuerResource, IssuerResourceDoneable>>
        client;

    public IssuerClient(
        final MixedOperation<IssuerResource, IssuerResourceList, IssuerResourceDoneable, Resource<IssuerResource, IssuerResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<IssuerResource, IssuerResourceList, IssuerResourceDoneable, Resource<IssuerResource, IssuerResourceDoneable>> client()
    {
        return client;
    }
}
