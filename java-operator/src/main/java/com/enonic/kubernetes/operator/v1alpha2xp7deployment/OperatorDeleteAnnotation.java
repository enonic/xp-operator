package com.enonic.kubernetes.operator.v1alpha2xp7deployment;

import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static com.enonic.kubernetes.kubernetes.Predicates.*;


/**
 * This operator class deletes resources that are annotated with the delete annotation
 */
@ApplicationScoped
public class OperatorDeleteAnnotation
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @ConfigProperty(name = "operator.charts.values.annotationKeys.removeWithDeployment")
    String deleteAnnotation;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.xp7DeploymentInformer() );
    }

    @Override
    public void onNewAdd( final Xp7Deployment newResource )
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
        String namespace = oldResource.getMetadata().getNamespace();
        String name = oldResource.getMetadata().getName();

        deleteDomains( name );
        deleteNamespaces( namespace, name );
    }

    private void deleteDomains( final String name )
    {
        // Delete all annotated domains
        searchers.domain().stream().
            filter( isDeleted().negate() ).
            filter( matchAnnotation( deleteAnnotation, name ) ).
            forEach( d -> K8sLogHelper.logDelete( clients.domain().
                withName( d.getMetadata().getName() ) ) );
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
