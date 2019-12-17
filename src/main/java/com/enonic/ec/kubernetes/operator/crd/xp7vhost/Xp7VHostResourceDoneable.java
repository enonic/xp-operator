package com.enonic.ec.kubernetes.operator.crd.xp7vhost;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

@SuppressWarnings("WeakerAccess")
public class Xp7VHostResourceDoneable
    extends CustomResourceDoneable<Xp7VHostResource>
{
    public Xp7VHostResourceDoneable( Xp7VHostResource resource, Function<Xp7VHostResource, Xp7VHostResource> function )
    {
        super( resource, function );
    }
}