package com.enonic.cloud.operator.helpers;

import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.functions.OptionalListPruner;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.helm.functions.HelmToK8s;
import com.enonic.cloud.helm.functions.HelmToK8sImpl;
import com.enonic.cloud.helm.functions.HelmToK8sParamsImpl;
import com.enonic.cloud.helm.functions.K8sCommandBuilder;
import com.enonic.cloud.helm.functions.K8sCommandSorter;
import com.enonic.cloud.helm.functions.Templator;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;

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
    public void init()
    {
        helmToK8s = HelmToK8sImpl.of( k8sCommandMapper, getValueBuilder( baseValues ), getTemplator() );
    }

    @Override
    public void onNewAdd( final R newResource )
    {
        handle( null, newResource );
    }

    @Override
    public void onUpdate( final R oldResource, final R newResource )
    {
        if ( !specEquals( oldResource, newResource ) )
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
    }

    protected abstract ValueBuilder<R> getValueBuilder( final BaseValues baseValues );

    protected abstract Templator getTemplator();

    protected abstract boolean specEquals( final R oldResource, final R newResource );
}
