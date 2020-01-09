package com.enonic.ec.kubernetes.operator.operators.v1alpha1.api.admission;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client.Xp7VHostCache;

public class TestXp7VHostCache
    extends Xp7VHostCache
{
    public TestXp7VHostCache()
    {
        super();
    }

    public void put( Xp7VHostResource resource )
    {
        this.cache.put( resource.getMetadata().getUid(), resource );
    }
}
