package com.enonic.ec.kubernetes.operator.crd.app;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class XpAppResourceDoneable
    extends CustomResourceDoneable<XpAppResource>
{
    public XpAppResourceDoneable( XpAppResource resource, Function<XpAppResource, XpAppResource> function )
    {
        super( resource, function );
    }
}