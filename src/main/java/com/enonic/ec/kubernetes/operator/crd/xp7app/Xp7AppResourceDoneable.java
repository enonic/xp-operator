package com.enonic.ec.kubernetes.operator.crd.xp7app;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class Xp7AppResourceDoneable
    extends CustomResourceDoneable<Xp7AppResource>
{
    public Xp7AppResourceDoneable( Xp7AppResource resource, Function<Xp7AppResource, Xp7AppResource> function )
    {
        super( resource, function );
    }
}