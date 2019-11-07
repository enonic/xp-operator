package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResource;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResourceList;

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
