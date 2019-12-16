package com.enonic.ec.kubernetes.operator.api.admission;

import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;

public class TestDeploymentCache
    extends XpDeploymentCache
{
    public TestDeploymentCache()
    {
        super();
    }

    public void put( XpDeploymentResource resource )
    {
        this.cache.put( resource.getMetadata().getUid(), resource );
    }
}
