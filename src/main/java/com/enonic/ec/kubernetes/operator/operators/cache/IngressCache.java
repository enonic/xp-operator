package com.enonic.ec.kubernetes.operator.operators.cache;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.common.resources.Cache;

public class IngressCache
    extends Cache<Ingress>
{
    private Clients clients;

    @Inject
    public IngressCache( Clients clients )
    {
        this.clients = clients;
    }

    @Override
    protected Class<Ingress> getResourceClass()
    {
        return Ingress.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FilterWatchListDeletable filter()
    {
        return clients.getDefaultClient().
            extensions().
            ingresses().
            inAnyNamespace();
    }
}
