package com.enonic.ec.kubernetes.operator.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.client.DefaultClientProducer;

@Singleton
public class ConfigMapCache
    extends ResourceCache<ConfigMap, ConfigMapList>
{
    @Inject
    public ConfigMapCache( DefaultClientProducer defaultClientProducer )
    {
        super( getResourceFilter( defaultClientProducer.client() ) );
    }

    private static FilterWatchListDeletable<ConfigMap, ConfigMapList, Boolean, Watch, Watcher<ConfigMap>> getResourceFilter(
        KubernetesClient client )
    {
        return client.configMaps().inAnyNamespace().withLabel( cfgStr( "operator.deployment.xp.labels.config.managed" ), "true" );
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final ConfigMap resource )
            {
                watcherHandleEvent( action, resource );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
