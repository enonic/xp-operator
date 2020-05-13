package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;

@Singleton
public class NamespaceCache
    extends AbstractCache<Namespace, NamespaceList>
{
    @Inject
    public NamespaceCache( KubernetesClient client )
    {
        super( client.
            namespaces() );
    }
}
