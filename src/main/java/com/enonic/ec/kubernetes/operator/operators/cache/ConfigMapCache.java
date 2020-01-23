package com.enonic.ec.kubernetes.operator.operators.cache;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.common.resources.Cache;

public class ConfigMapCache
    extends Cache<ConfigMap>
{
    private Clients clients;

    @Inject
    public ConfigMapCache( Clients clients )
    {
        this.clients = clients;
    }

    @Override
    protected Class<ConfigMap> getResourceClass()
    {
        return ConfigMap.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FilterWatchListDeletable filter()
    {
        return clients.
            getDefaultClient().
            configMaps().
            inAnyNamespace();
    }
}
