package com.enonic.ec.kubernetes.crd.vhost;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class XpVHostResourceDoneable
    extends CustomResourceDoneable<XpVHostResource>
{
    public XpVHostResourceDoneable( XpVHostResource resource, Function<XpVHostResource, XpVHostResource> function )
    {
        super( resource, function );
    }
}