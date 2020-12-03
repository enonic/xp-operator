package com.enonic.cloud.operator.v1alpha2xp7deployment;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.kubernetes.Predicates.isDeleted;
import static com.enonic.cloud.kubernetes.Predicates.matchAnnotation;


/**
 * This operator class deletes resources that are annotated with the delete annotation
 */
@Singleton
public class OperatorDeleteAnnotation
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @ConfigProperty(name = "operator.charts.values.annotationKeys.removeWithDeployment")
    String deleteAnnotation;

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
        // Get name
        String name = oldResource.getMetadata().getName();

        deleteDomains( name );
        deleteNamespaces( name );
    }

    private void deleteDomains( final String name )
    {
        // Delete all annotated domains
        searchers.domain().stream().
            filter( isDeleted().negate() ).
            filter( matchAnnotation( deleteAnnotation, name ) ).
            forEach( d -> K8sLogHelper.logDelete( clients.domain().crdClient().
                withName( d.getMetadata().getName() ) ) );
    }

    private void deleteNamespaces( final String name )
    {
        // Delete all annotated namespaces
        searchers.namespace().stream().
            filter( isDeleted().negate() ).
            filter( matchAnnotation( deleteAnnotation, name ) ).
            forEach( ns -> K8sLogHelper.logDelete( clients.k8s().namespaces().
                withName( ns.getMetadata().getName() ) ) );
    }
}
