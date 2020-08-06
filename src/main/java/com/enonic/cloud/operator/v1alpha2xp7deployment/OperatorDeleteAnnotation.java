package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.Namespace;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.helpers.InformerEventHandler;


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

    @ConfigProperty(name = "operator.annotations.delete")
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
        // Collect annotated domains
        List<Domain> domains = searchers.domain().query().
            hasNotBeenDeleted().
            hasAnnotation( deleteAnnotation, name ).
            list();

        domains.forEach( d -> K8sLogHelper.logDelete( clients.domain().crdClient().
            withName( d.getMetadata().getName() ) ) );
    }

    private void deleteNamespaces( final String name )
    {
        // Collect annotated namespaces
        List<Namespace> namespaces = searchers.namespace().query().
            hasNotBeenDeleted().
            hasAnnotation( deleteAnnotation, name ).
            list();

        // Delete those namespace
        namespaces.forEach( ns -> K8sLogHelper.logDelete( clients.k8s().
            namespaces().
            withName( ns.getMetadata().getName() ) ) );
    }
}
