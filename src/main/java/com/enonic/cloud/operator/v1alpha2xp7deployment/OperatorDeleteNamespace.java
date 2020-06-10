package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.functions.OptionalListPruner;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.kubernetes.caches.NamespaceCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7DeploymentCache;
import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

import static com.enonic.cloud.common.Configuration.cfgStr;


@ApplicationScoped
public class OperatorDeleteNamespace
    implements ResourceEventHandler<V1alpha2Xp7Deployment>
{
    @Inject
    NamespaceCache namespaceCache;

    @Inject
    KubernetesClient client;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7DeploymentCache v1alpha2Xp7DeploymentCache;

    @Inject
    K8sCommandMapper k8sCommandMapper;

    @Inject
    OptionalListPruner<K8sCommand> listPruner;

    @Inject
    RunnableListExecutor runnableListExecutor;

    void onStartup( @Observes StartupEvent _ev )
    {
        v1alpha2Xp7DeploymentCache.addEventListener( this );
    }

    @Override
    public void onAdd( final V1alpha2Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final V1alpha2Xp7Deployment oldResource, final V1alpha2Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final V1alpha2Xp7Deployment oldResource, final boolean b )
    {
        collectAnnotatedNamespace().
            andThen( this::deleteNamespaces ).
            apply( oldResource );
    }

    private Function<V1alpha2Xp7Deployment, List<Namespace>> collectAnnotatedNamespace()
    {
        return ( resource ) -> namespaceCache.getStream().
            // Get namespaces with annotations
                filter( namespace -> namespace.getMetadata().getDeletionTimestamp() != null ).
                filter( namespace -> namespace.getMetadata().getAnnotations() != null ).
            // Get with delete annotation matching this deployment
                filter( namespace -> Objects.equals(
                namespace.getMetadata().getAnnotations().get( cfgStr( "operator.namespace.delete.annotation" ) ),
                resource.getMetadata().getName() ) ).
            // Get only those that are not terminating
                filter( namespace -> !Objects.equals( namespace.getStatus().getPhase(), "Terminating" ) ).
            // Collect
                collect( Collectors.toList() );
    }

    private Void deleteNamespaces( final List<Namespace> namespaces )
    {
        client.namespaces().delete( namespaces );
        return null;
    }
}
