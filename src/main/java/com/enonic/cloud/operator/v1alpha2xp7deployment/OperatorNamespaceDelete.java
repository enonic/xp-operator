package com.enonic.cloud.operator.v1alpha2xp7deployment;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Namespace;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


@Singleton
public class OperatorNamespaceDelete
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    protected void init()
    {
        // Do nothing
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
        searchers.namespace().query().
            hasNotBeenDeleted().
            hasAnnotation( cfgStr( "operator.namespace.delete.annotation" ), oldResource.getMetadata().getName() ).
            forEach( this::deleteNamespace );
    }

    private void deleteNamespace( final Namespace namespace )
    {
        K8sLogHelper.logDelete( clients.k8s().namespaces().withName( namespace.getMetadata().getName() ) );
    }
}
