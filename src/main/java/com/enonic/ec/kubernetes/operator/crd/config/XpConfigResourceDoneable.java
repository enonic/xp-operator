package com.enonic.ec.kubernetes.operator.crd.config;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class XpConfigResourceDoneable
    extends CustomResourceDoneable<XpConfigResource>
{
    public XpConfigResourceDoneable( XpConfigResource resource, Function<XpConfigResource, XpConfigResource> function )
    {
        super( resource, function );
    }
}