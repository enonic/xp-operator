package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

@Singleton
public class ConfigMapCache
    extends Cache<ConfigMap, ConfigMapList>
{
    private Clients clients;

    @Inject
    public ConfigMapCache( Clients clients )
    {
        this.clients = clients;
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        FilterWatchListDeletable<ConfigMap, ConfigMapList, Boolean, Watch, Watcher<ConfigMap>> filter = clients.
            getDefaultClient().
            configMaps().
            inAnyNamespace().
            withLabel( "managedBy", "ec-operator" );

        startWatcher( filter, new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final ConfigMap configMap )
            {
                watcherEventRecieved( action, configMap );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
