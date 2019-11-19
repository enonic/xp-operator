package com.enonic.ec.kubernetes.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;

@Singleton
public class ConfigMapCache
    extends ResourceCache<ConfigMap>
{
    private final KubernetesClient client;

    @Inject
    public ConfigMapCache( DefaultClientProducer defaultClientProducer )
    {
        super();
        this.client = defaultClientProducer.client();
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        // Set initial state of config maps
        initialize( client.configMaps().inAnyNamespace().list().getItems() );

        // Only watch XP config maps
        client.configMaps().inAnyNamespace().withLabel( cfgStr( "operator.deployment.xp.labels.ec.type" ), "xp" ).watch( new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final ConfigMap configMap )
            {
                handleEvent( action, configMap );
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
