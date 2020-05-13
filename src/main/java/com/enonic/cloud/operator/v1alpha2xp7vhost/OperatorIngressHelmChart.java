package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.functions.OptionalListPruner;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.helm.functions.HelmToK8s;
import com.enonic.cloud.helm.functions.HelmToK8sImpl;
import com.enonic.cloud.helm.functions.HelmToK8sParamsImpl;
import com.enonic.cloud.helm.functions.K8sCommandBuilder;
import com.enonic.cloud.helm.functions.Templator;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7VHostCache;
import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;


public class OperatorIngressHelmChart
    implements ResourceEventHandler<V1alpha2Xp7VHost>
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7VHostCache v1alpha2Xp7VHostCache;

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

    private HelmToK8s<V1alpha2Xp7VHost> helmToK8s;

    void onStartup( @Observes StartupEvent _ev )
    {
        helmToK8s = HelmToK8sImpl.of( k8sCommandMapper, VHostHelmValueBuilderImpl.of( baseValues ), templator );
        v1alpha2Xp7VHostCache.addEventListener( this );
    }

    @Override
    public void onAdd( final V1alpha2Xp7VHost newResource )
    {
        handle( null, newResource );
    }

    @Override
    public void onUpdate( final V1alpha2Xp7VHost oldResource, final V1alpha2Xp7VHost newResource )
    {
        if ( Objects.equals( oldResource.getSpec(), newResource.getSpec() ) )
        {
            return;
        }
        handle( oldResource, newResource );
    }

    @Override
    public void onDelete( final V1alpha2Xp7VHost oldResource, final boolean b )
    {
        handle( oldResource, null );
    }

    private void handle( final V1alpha2Xp7VHost oldResource, final V1alpha2Xp7VHost newResource )
    {
        helmToK8s.
            andThen( k8sCommandBuilder ).
            andThen( listPruner ).
            andThen( runnableListExecutor ).
            apply( HelmToK8sParamsImpl.of( Optional.ofNullable( oldResource ), Optional.ofNullable( newResource ) ) );
    }
}
