package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha1Xp7AppList;

@Singleton
public class AppCache
    extends Cache<V1alpha1Xp7App, V1alpha1Xp7AppList>
{
    private final Clients clients;

    @Inject
    public AppCache( Clients clients )
    {
        super( defaultExecutorService );
        this.clients = clients;
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        FilterWatchListMultiDeletable<V1alpha1Xp7App, V1alpha1Xp7AppList, Boolean, Watch, Watcher<V1alpha1Xp7App>> filter = clients.
            getAppClient().
            inAnyNamespace();
        startWatcher( filter, new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final V1alpha1Xp7App v1alpha1Xp7App )
            {
                watcherEventReceived( action, v1alpha1Xp7App );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
