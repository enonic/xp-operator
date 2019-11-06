package com.enonic.ec.kubernetes.crd.vhost.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResourceDoneable;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResourceList;

public class XpVHostClient
{
    private final MixedOperation<XpVHostResource, XpVHostResourceList, XpVHostResourceDoneable, Resource<XpVHostResource, XpVHostResourceDoneable>>
        client;

    public XpVHostClient(
        final MixedOperation<XpVHostResource, XpVHostResourceList, XpVHostResourceDoneable, Resource<XpVHostResource, XpVHostResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<XpVHostResource, XpVHostResourceList, XpVHostResourceDoneable, Resource<XpVHostResource, XpVHostResourceDoneable>> client()
    {
        return client;
    }
}
