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
    extends ResourceCache<Pod>
{
    private final KubernetesClient client;

    @Inject
    public PodCache( DefaultClientProducer defaultClientProducer )
    {
        super();
        this.client = defaultClientProducer.client();
    }

    private FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> getResourceFilter()
    {
        return client.pods().inAnyNamespace().withLabel( cfgStr( "operator.deployment.xp.labels.pod.managed" ), "true" );
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        // Set initial state of pods
        initialize( getResourceFilter().list().getItems() );

        // Only watch XP config maps
        getResourceFilter().watch( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Pod pod )
            {
                handleEvent( action, pod );
            }

            @Override
            public void onClose( final KubernetesClientException cause )
            {
                if ( cause != null )
                {
                    // This means the socket closed and we have a problem, best to
                    // let kubernetes just restart the operator pod.
                    cause.printStackTrace();
                    System.exit( -1 );
                }
            }
        } );
    }
}
