package com.enonic.cloud.operator.api.admission;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.operators.common.cache.VHostCache;

public class TestXp7VHostCache
    extends VHostCache
{
    public TestXp7VHostCache()
    {
        super();
    }

    public void put( V1alpha2Xp7VHost resource )
    {
        this.cache.put( resource.getMetadata().getUid(), resource );
    }
}