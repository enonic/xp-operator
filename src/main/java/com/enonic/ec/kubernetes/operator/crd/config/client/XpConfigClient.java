package com.enonic.ec.kubernetes.operator.crd.config.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResourceList;

public class XpConfigClient
{
    private final MixedOperation<XpConfigResource, XpConfigResourceList, XpConfigResourceDoneable, Resource<XpConfigResource, XpConfigResourceDoneable>>
        client;

    public XpConfigClient(
        final MixedOperation<XpConfigResource, XpConfigResourceList, XpConfigResourceDoneable, Resource<XpConfigResource, XpConfigResourceDoneable>> client )
    {
        this.client = client;
    }

    public MixedOperation<XpConfigResource, XpConfigResourceList, XpConfigResourceDoneable, Resource<XpConfigResource, XpConfigResourceDoneable>> client()
    {
        return client;
    }
}
