package com.enonic.ec.kubernetes.operator.operators.cache;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.common.resources.Cache;

public class VHostCache
    extends Cache<V1alpha2Xp7VHost>
{
    private Clients clients;

    @Inject
    public VHostCache( Clients clients )
    {
        this.clients = clients;
    }

    @Override
    protected Class<V1alpha2Xp7VHost> getResourceClass()
    {
        return V1alpha2Xp7VHost.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FilterWatchListDeletable filter()
    {
        return clients.
            getVHostClient().
            inAnyNamespace();
    }
}
