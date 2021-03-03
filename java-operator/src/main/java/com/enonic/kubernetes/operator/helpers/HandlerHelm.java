package com.enonic.kubernetes.operator.helpers;

import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.kubernetes.common.functions.OptionalListPruner;
import com.enonic.kubernetes.common.functions.RunnableListExecutor;
import com.enonic.kubernetes.helm.functions.HelmToK8s;
import com.enonic.kubernetes.helm.functions.HelmToK8sImpl;
import com.enonic.kubernetes.helm.functions.HelmToK8sParamsImpl;
import com.enonic.kubernetes.helm.functions.K8sCommandBuilder;
import com.enonic.kubernetes.helm.functions.K8sCommandSorter;
import com.enonic.kubernetes.helm.functions.Templator;
import com.enonic.kubernetes.helm.values.BaseValues;
import com.enonic.kubernetes.helm.values.ValueBuilder;
import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandMapper;

import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.isBeingBackupRestored;

public abstract class HandlerHelm<R extends HasMetadata>
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

    private HelmToK8s<R> helmToK8s;

    @Override
    public void initialize()
    {
        super.initialize();
        helmToK8s = HelmToK8sImpl.of( k8sCommandMapper, getValueBuilder( baseValues ), getTemplator() );
    }

    @Override
    public void onNewAdd( final R newResource )
    {
        if ( isBeingBackupRestored().negate().test( newResource ) )
        {
            handle( null, newResource );
        }
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
        helmToK8s.
            andThen( k8sCommandBuilder ).
            andThen( listPruner ).
            andThen( commandSorter ).
            andThen( runnableListExecutor ).
            apply( HelmToK8sParamsImpl.of( Optional.ofNullable( oldResource ), Optional.ofNullable( newResource ) ) );

        postHandle( newResource.getMetadata().getNamespace() );
    }

    protected void postHandle( final String namespace )
    {
        // By default do nothing
    }

    protected abstract ValueBuilder<R> getValueBuilder( final BaseValues baseValues );

    protected abstract Templator getTemplator();
}
