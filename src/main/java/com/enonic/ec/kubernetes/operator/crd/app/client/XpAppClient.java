package com.enonic.ec.kubernetes.operator.crd.app.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResourceList;

public class XpAppClient
{
    private final MixedOperation<XpAppResource, XpAppResourceList, XpAppResourceDoneable, Resource<XpAppResource, XpAppResourceDoneable>>
        client;

    public XpAppClient(
        final MixedOperation<XpAppResource, XpAppResourceList, XpAppResourceDoneable, Resource<XpAppResource, XpAppResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<XpAppResource, XpAppResourceList, XpAppResourceDoneable, Resource<XpAppResource, XpAppResourceDoneable>> client()
    {
        return client;
    }
}
