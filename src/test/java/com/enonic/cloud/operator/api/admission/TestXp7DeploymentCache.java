package com.enonic.cloud.operator.api.admission;

import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7DeploymentCache;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

public class TestXp7DeploymentCache
    extends V1alpha2Xp7DeploymentCache
{
    public TestXp7DeploymentCache()
    {
        super();
    }

    public void put( V1alpha2Xp7Deployment resource )
    {
        this.cache.put( resource.getMetadata().getUid(), resource );
    }
}
