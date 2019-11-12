package com.enonic.ec.kubernetes.crd.issuer;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class IssuerResourceDoneable
    extends CustomResourceDoneable<IssuerResource>
{
    public IssuerResourceDoneable( IssuerResource resource, Function<IssuerResource, IssuerResource> function )
    {
        super( resource, function );
    }
}