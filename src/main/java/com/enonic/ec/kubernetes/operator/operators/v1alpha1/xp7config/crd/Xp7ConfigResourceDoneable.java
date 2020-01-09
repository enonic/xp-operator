package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd;

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