package com.enonic.cloud.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;

import com.enonic.cloud.operator.operators.common.clients.Clients;

@Singleton
public class NamespaceCache
    extends Cache<Namespace, NamespaceList>
{
    @Inject
    public NamespaceCache( Clients clients )
    {
        super( clients.getDefaultClient().namespaces() );
    }
}
