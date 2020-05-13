package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.client.V1alpha1Xp7AppList;

@Singleton
public class V1alpha1Xp7AppCache
    extends AbstractCache<V1alpha1Xp7App, V1alpha1Xp7AppList>
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public V1alpha1Xp7AppCache( CrdClient client )
    {
        super( client.
            xp7Apps().
            inAnyNamespace() );
    }
}
