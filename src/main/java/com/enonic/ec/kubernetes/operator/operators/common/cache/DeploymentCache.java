package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha2Xp7DeploymentList;

@Singleton
public class DeploymentCache
    extends Cache<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList>
{
    protected DeploymentCache()
    {
        super();
    }

    @Inject
    public DeploymentCache( Clients clients )
    {
        super( clients.getDeploymentClient().inAnyNamespace() );
    }
}
