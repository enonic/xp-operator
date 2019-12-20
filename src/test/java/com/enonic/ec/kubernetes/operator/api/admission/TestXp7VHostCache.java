package com.enonic.ec.kubernetes.operator.api.admission;

import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.client.Xp7VHostCache;

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
