package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.client.V1alpha2Xp7ConfigList;

@Singleton
public class V1alpha2Xp7ConfigCache
    extends AbstractCache<V1alpha2Xp7Config, V1alpha2Xp7ConfigList>
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public V1alpha2Xp7ConfigCache( CrdClient client )
    {
        super( client.
            xp7Configs().
            inAnyNamespace() );
    }
}
