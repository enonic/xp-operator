package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.functions.OptionalListPruner;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.helm.functions.HelmToK8s;
import com.enonic.cloud.helm.functions.HelmToK8sImpl;
import com.enonic.cloud.helm.functions.HelmToK8sParamsImpl;
import com.enonic.cloud.helm.functions.K8sCommandBuilder;
import com.enonic.cloud.helm.functions.Templator;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.operator.InformerEventHandler;


public class OperatorIngressHelmChart
    extends InformerEventHandler<Xp7VHost>
{
    @Inject
    SharedIndexInformer<Xp7VHost> xp7VHostSharedIndexInformer;

    @Inject
    K8sCommandMapper k8sCommandMapper;

    @Inject
    @Named("v1alpha2/xp7vhost")
    Templator templator;

    @Inject
    BaseValues baseValues;

    @Inject
    K8sCommandBuilder k8sCommandBuilder;

    @Inject
    OptionalListPruner<K8sCommand> listPruner;

    @Inject
    RunnableListExecutor runnableListExecutor;

    private HelmToK8s<Xp7VHost> helmToK8s;

    void onStartup( @Observes StartupEvent _ev )
    {
        helmToK8s = HelmToK8sImpl.of( k8sCommandMapper, VHostHelmValueBuilderImpl.of( baseValues ), templator );
        listenToInformer( xp7VHostSharedIndexInformer );
    }

    @Override
    public void onAdd( final Xp7VHost newResource )
    {
        handle( null, newResource );
    }

    @Override
    public void onUpdate( final Xp7VHost oldResource, final Xp7VHost newResource )
    {
        if ( Objects.equals( oldResource.getXp7VHostSpec(), newResource.getXp7VHostSpec() ) )
        {
            return;
        }
        handle( oldResource, newResource );
    }

    @Override
    public void onDelete( final Xp7VHost oldResource, final boolean b )
    {
        handle( oldResource, null );
    }

    private void handle( final Xp7VHost oldResource, final Xp7VHost newResource )
    {
        helmToK8s.
            andThen( k8sCommandBuilder ).
            andThen( listPruner ).
            andThen( runnableListExecutor ).
            apply( HelmToK8sParamsImpl.of( Optional.ofNullable( oldResource ), Optional.ofNullable( newResource ) ) );
    }
}
