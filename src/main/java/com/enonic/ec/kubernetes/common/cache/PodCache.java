package com.enonic.ec.kubernetes.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;

@Singleton
public class PodCache
    extends ResourceCache<Pod, PodList>
{
    @Inject
    public PodCache( DefaultClientProducer defaultClientProducer )
    {
        super( getResourceFilter( defaultClientProducer.client() ) );
    }

    private static FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> getResourceFilter( KubernetesClient client )
    {
        return client.pods().inAnyNamespace().withLabel( cfgStr( "operator.deployment.xp.labels.pod.managed" ), "true" );
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        startWatcher( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Pod resource )
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
