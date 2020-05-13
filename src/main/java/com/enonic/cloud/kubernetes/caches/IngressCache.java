package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.KubernetesClient;

@Singleton
public class IngressCache
    extends AbstractCache<Ingress, IngressList>
{
    @Inject
    public IngressCache( KubernetesClient client )
    {
        super( client.
            extensions().
            ingresses().
            inAnyNamespace() );
    }
}
