package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.client.V1alpha2Xp7VHostList;

@Singleton
public class V1alpha2Xp7VHostCache
    extends AbstractCache<V1alpha2Xp7VHost, V1alpha2Xp7VHostList>
{
    // Only for testing
    protected V1alpha2Xp7VHostCache()
    {
        super();
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public V1alpha2Xp7VHostCache( CrdClient client )
    {
        super( client.
            xp7VHosts().
            inAnyNamespace() );
    }
}
