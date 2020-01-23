package com.enonic.ec.kubernetes.operator.operators.cache;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.common.resources.Cache;

public class ConfigCache
    extends Cache<V1alpha2Xp7Config>
{
    private Clients clients;

    @Inject
    public ConfigCache( Clients clients )
    {
        this.clients = clients;
    }

    @Override
    protected Class<V1alpha2Xp7Config> getResourceClass()
    {
        return V1alpha2Xp7Config.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FilterWatchListDeletable filter()
    {
        return clients.
            getConfigClient().
            inAnyNamespace();
    }
}
