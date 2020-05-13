package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.client.V1alpha2Xp7DeploymentList;

@Singleton
public class V1alpha2Xp7DeploymentCache
    extends AbstractCache<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList>
{
    // Only for testing
    protected V1alpha2Xp7DeploymentCache()
    {
        super();
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public V1alpha2Xp7DeploymentCache( CrdClient client )
    {
        super( client.
            xp7Deployments().
            inAnyNamespace() );
    }
}
