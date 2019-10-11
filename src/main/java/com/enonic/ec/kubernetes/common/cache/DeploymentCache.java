//package com.enonic.ec.kubernetes.common.cache;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.enterprise.event.Observes;
//import javax.inject.Inject;
//
//import io.fabric8.kubernetes.api.model.apps.Deployment;
//import io.fabric8.kubernetes.client.KubernetesClient;
//import io.fabric8.kubernetes.client.KubernetesClientException;
//import io.fabric8.kubernetes.client.Watcher;
//import io.quarkus.runtime.StartupEvent;
//
//@ApplicationScoped
//public class DeploymentCache
//    extends ResourceCache<Deployment>
//{
//    @Inject
//    KubernetesClient client;
//
//    void onStartup( @Observes StartupEvent _ev )
//    {
//        initialize( client.apps().deployments().list().getItems() );
//        client.apps().deployments().watch( new Watcher<Deployment>()
//        {
//            @Override
//            public void eventReceived( final Action action, final Deployment deployment )
//            {
//                handleEvent( action, deployment );
//            }
//
//            @Override
//            public void onClose( final KubernetesClientException cause )
//            {
//                // This means the socket closed and we have a problem, best to let kubernetes
//                // just restart the controller pod.
//                cause.printStackTrace();
//                System.exit( -1 );
//            }
//        } );
//    }
//}
