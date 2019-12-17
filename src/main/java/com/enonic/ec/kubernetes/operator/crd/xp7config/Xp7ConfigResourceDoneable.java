package com.enonic.ec.kubernetes.operator.crd.xp7config;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class Xp7ConfigResourceDoneable
    extends CustomResourceDoneable<Xp7ConfigResource>
{
    public Xp7ConfigResourceDoneable( Xp7ConfigResource resource, Function<Xp7ConfigResource, Xp7ConfigResource> function )
    {
        super( resource, function );
    }
}