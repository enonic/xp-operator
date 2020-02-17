package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha2Xp7VHostList;

@Singleton
public class VHostCache
    extends Cache<V1alpha2Xp7VHost, V1alpha2Xp7VHostList>
{
    private final Clients clients;

    @Inject
    public VHostCache( Clients clients )
    {
        super( defaultExecutorService );
        this.clients = clients;
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        FilterWatchListMultiDeletable<V1alpha2Xp7VHost, V1alpha2Xp7VHostList, Boolean, Watch, Watcher<V1alpha2Xp7VHost>> filter = clients.
            getVHostClient().
            inAnyNamespace();
        startWatcher( filter, new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final V1alpha2Xp7VHost v1alpha2Xp7VHost )
            {
                watcherEventReceived( action, v1alpha2Xp7VHost );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
