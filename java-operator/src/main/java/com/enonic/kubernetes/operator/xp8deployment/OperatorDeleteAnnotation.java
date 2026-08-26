package com.enonic.kubernetes.operator.xp8deployment;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.enonic.kubernetes.kubernetes.Predicates.*;


/**
 * This operator class deletes resources that are annotated with the delete annotation
 */
@Singleton
public class OperatorDeleteAnnotation
    extends InformerEventHandler<Xp8Deployment>
    implements ApplicationEventListener<ServerStartupEvent>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Value("${operator.charts.values.annotationKeys.removeWithDeployment}")
    String deleteAnnotation;

    @Inject
    Informers informers;

    @Override
    public void onApplicationEvent( ServerStartupEvent event )
    {
        listen( informers.xp8DeploymentInformer() );
    }

    @Override
    public void onNewAdd( final Xp8Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp8Deployment oldResource, final Xp8Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final Xp8Deployment oldResource, final boolean b )
    {
        String namespace = oldResource.getMetadata().getNamespace();
        String name = oldResource.getMetadata().getName();

        deleteHazelcastClusterRoleBinding( namespace );
        deleteNamespaces( namespace, name );
    }

    private void deleteHazelcastClusterRoleBinding( final String deploymentNamespace )
    {
        // Kubernetes GC does not clean up cluster-scoped resources owned by namespace-scoped objects,
        // so explicitly delete the hazelcast ClusterRoleBinding on deployment removal.
        String crbName = deploymentNamespace + "-hazelcast";
        var resource = clients.k8s().rbac().clusterRoleBindings().withName( crbName );
        if (resource.get() != null) {
            K8sLogHelper.logDelete( resource );
        }
    }

    private void deleteNamespaces( final String deploymentNamespace, final String name )
    {
        // Delete all annotated namespaces
        searchers.namespace().stream().
            filter( withName(deploymentNamespace) ).
            filter( isDeleted().negate() ).
            filter( matchAnnotation( deleteAnnotation, name ) ).
            forEach( ns -> K8sLogHelper.logDelete( clients.k8s().namespaces().
                withName( ns.getMetadata().getName() ) ) );
    }
}
