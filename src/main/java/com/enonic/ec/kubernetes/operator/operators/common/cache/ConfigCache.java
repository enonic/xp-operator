package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha2Xp7ConfigList;

@Singleton
public class ConfigCache
    extends Cache<V1alpha2Xp7Config, V1alpha2Xp7ConfigList>
{
    private Clients clients;

    @Inject
    public ConfigCache( Clients clients )
    {
        this.clients = clients;
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        FilterWatchListMultiDeletable<V1alpha2Xp7Config, V1alpha2Xp7ConfigList, Boolean, Watch, Watcher<V1alpha2Xp7Config>> filter =
            clients.
                getConfigClient().
                inAnyNamespace();
        startWatcher( filter, new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final V1alpha2Xp7Config v1alpha2Xp7Config )
            {
                watcherEventRecieved( action, v1alpha2Xp7Config );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
