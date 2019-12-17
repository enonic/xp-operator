package com.enonic.ec.kubernetes.operator.api.admission;

import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.client.Xp7DeploymentCache;

public class Test7DeploymentCache
    extends Xp7DeploymentCache
{
    public Test7DeploymentCache()
    {
        super();
    }

    public void put( Xp7DeploymentResource resource )
    {
        this.cache.put( resource.getMetadata().getUid(), resource );
    }
}
