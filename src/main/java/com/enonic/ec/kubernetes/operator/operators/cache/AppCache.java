package com.enonic.ec.kubernetes.operator.operators.cache;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.common.resources.Cache;

public class AppCache
    extends Cache<V1alpha1Xp7App>
{
    private Clients clients;

    @Inject
    public AppCache( Clients clients )
    {
        this.clients = clients;
    }

    @Override
    protected Class<V1alpha1Xp7App> getResourceClass()
    {
        return V1alpha1Xp7App.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FilterWatchListDeletable filter()
    {
        return clients.
            getAppClient().
            inAnyNamespace();
    }
}
