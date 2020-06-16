package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


@ApplicationScoped
public class OperatorDeleteNamespace
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    Clients clients;

    @Inject
    SharedIndexInformer<Xp7Deployment> xp7DeploymentSharedIndexInformer;

    void onStartup( @Observes StartupEvent _ev )
    {
        listenToInformer( xp7DeploymentSharedIndexInformer );
    }

    @Override
    public void onAdd( final Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final Xp7Deployment oldResource, final boolean b )
    {
        collectAnnotatedNamespace().
            andThen( this::deleteNamespaces ).
            apply( oldResource );
    }

    private Function<Xp7Deployment, List<Namespace>> collectAnnotatedNamespace()
    {
        return ( resource ) -> clients.k8s().namespaces().list().getItems().stream().
            // Get namespaces with annotations
                filter( namespace -> namespace.getMetadata().getDeletionTimestamp() == null ).
                filter( namespace -> namespace.getMetadata().getAnnotations() != null ).
            // Get with delete annotation matching this deployment
                filter( namespace -> Objects.equals(
                namespace.getMetadata().getAnnotations().get( cfgStr( "operator.namespace.delete.annotation" ) ),
                resource.getMetadata().getName() ) ).
            // Collect
                collect( Collectors.toList() );
    }

    private Void deleteNamespaces( final List<Namespace> namespaces )
    {
        for ( Namespace ns : namespaces )
        {
            K8sLogHelper.logDelete( clients.k8s().namespaces().withName( ns.getMetadata().getName() ) );
        }
        return null;
    }
}
