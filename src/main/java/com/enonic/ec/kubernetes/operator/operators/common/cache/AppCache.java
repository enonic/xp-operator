package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.Watcher;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha1Xp7AppList;

@Singleton
public class AppCache
    extends Cache<V1alpha1Xp7App, V1alpha1Xp7AppList>
    implements Watcher<V1alpha1Xp7App>
{
    @Inject
    public AppCache( Clients clients )
    {
        super( clients.getAppClient().inAnyNamespace() );
    }
}
