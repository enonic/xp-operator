package com.enonic.kubernetes.operator.helpers;

import java.util.Optional;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.kubernetes.common.functions.OptionalListPruner;
import com.enonic.kubernetes.common.functions.RunnableListExecutor;
import com.enonic.kubernetes.templates.functions.TemplateToK8s;
import com.enonic.kubernetes.templates.functions.TemplateToK8sParams;
import com.enonic.kubernetes.templates.functions.K8sCommandBuilder;
import com.enonic.kubernetes.templates.functions.K8sCommandSorter;
import com.enonic.kubernetes.templates.functions.Templator;
import com.enonic.kubernetes.templates.values.BaseValues;
import com.enonic.kubernetes.templates.values.ValueBuilder;
import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandMapper;

import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;

public abstract class HandlerTemplate<R extends HasMetadata>
    extends InformerEventHandler<R>
{
    @Inject
    public K8sCommandBuilder k8sCommandBuilder;

    @Inject
    public K8sCommandMapper k8sCommandMapper;

    @Inject
    public BaseValues baseValues;

    @Inject
    public OptionalListPruner<K8sCommand> listPruner;

    @Inject
    public RunnableListExecutor runnableListExecutor;

    @Inject
    public K8sCommandSorter commandSorter;

    private TemplateToK8s<R> templateToK8s;

    @Override
    public void initialize()
    {
        super.initialize();
        templateToK8s = new TemplateToK8s<>( k8sCommandMapper, getValueBuilder( baseValues ), getTemplator() );
    }

    @Override
    public void onNewAdd( final R newResource )
    {
        handle( null, newResource );
    }

    @Override
    public void onUpdate( final R oldResource, final R newResource )
    {
        if ( fieldEquals( (CustomResource) oldResource, CustomResource::getSpec ).negate().test( (CustomResource) newResource ) )
        {
            handle( oldResource, newResource );
        }
    }

    @Override
    public void onDelete( final R oldResource, final boolean b )
    {
        // Do nothing, this is handled by Kubernetes GC
    }

    private void handle( final R oldResource, final R newResource )
    {
        templateToK8s.
            andThen( k8sCommandBuilder ).
            andThen( listPruner ).
            andThen( commandSorter ).
            andThen( runnableListExecutor ).
            apply( new TemplateToK8sParams<>( Optional.ofNullable( oldResource ), Optional.ofNullable( newResource ) ) );

        postHandle( newResource.getMetadata().getNamespace() );
    }

    protected void postHandle( final String namespace )
    {
        // By default do nothing
    }

    protected abstract ValueBuilder<R> getValueBuilder( BaseValues baseValues );

    protected abstract Templator getTemplator();
}
