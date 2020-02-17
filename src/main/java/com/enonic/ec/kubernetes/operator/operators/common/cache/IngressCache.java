package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

@Singleton
public class IngressCache
    extends Cache<Ingress, IngressList>
{
    private final Clients clients;

    @Inject
    public IngressCache( Clients clients )
    {
        super( 1 );
        this.clients = clients;
    }

    protected void onStartup( @Observes StartupEvent _ev )
    {
        FilterWatchListMultiDeletable<Ingress, IngressList, Boolean, Watch, Watcher<Ingress>> filter = clients.getDefaultClient().
            extensions().
            ingresses().
            inAnyNamespace();
        startWatcher( filter, new Watcher<>()
        {
            @Override
            public void eventReceived( final Action action, final Ingress ingress )
            {
                watcherEventReceived( action, ingress );
            }

            @Override
            public void onClose( final KubernetesClientException e )
            {
                watcherOnClose( e );
            }
        } );
    }
}
