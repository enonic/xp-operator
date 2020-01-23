package com.enonic.ec.kubernetes.operator.operators.cache;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.common.resources.Cache;

public class DeploymentCache
    extends Cache<V1alpha2Xp7Deployment>
{
    private Clients clients;

    @Inject
    public DeploymentCache( Clients clients )
    {
        this.clients = clients;
    }

    @Override
    protected Class<V1alpha2Xp7Deployment> getResourceClass()
    {
        return V1alpha2Xp7Deployment.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FilterWatchListDeletable filter()
    {
        return clients.
            getDeploymentClient().
            inAnyNamespace();
    }
}
