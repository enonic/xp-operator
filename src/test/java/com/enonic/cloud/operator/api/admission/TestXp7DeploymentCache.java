package com.enonic.cloud.operator.api.admission;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.operators.common.cache.DeploymentCache;

public class TestXp7DeploymentCache
    extends DeploymentCache
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