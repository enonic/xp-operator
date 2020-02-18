package com.enonic.cloud.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;

import com.enonic.cloud.operator.operators.common.clients.Clients;

@Singleton
public class IngressCache
    extends Cache<Ingress, IngressList>
{
    @Inject
    public IngressCache( Clients clients )
    {
        super( clients.getDefaultClient().
            extensions().
            ingresses().
            inAnyNamespace() );
    }
}
