package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.functions.OptionalListPruner;
import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.helm.functions.HelmToK8s;
import com.enonic.cloud.helm.functions.HelmToK8sImpl;
import com.enonic.cloud.helm.functions.HelmToK8sParamsImpl;
import com.enonic.cloud.helm.functions.K8sCommandBuilder;
import com.enonic.cloud.helm.functions.K8sCommandSorter;
import com.enonic.cloud.helm.functions.Templator;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


@ApplicationScoped
public class OperatorDeploymentHelmChart
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    Clients clients;

    @Inject
    SharedIndexInformer<Xp7Deployment> xp7DeploymentSharedIndexInformer;

    @Inject
    K8sCommandMapper k8sCommandMapper;

    @Inject
    @Named("v1alpha2/xp7deployment")
    Templator templator;

    @Inject
    BaseValues baseValues;

    @Inject
    K8sCommandBuilder k8sCommandBuilder;

    @Inject
    OptionalListPruner<K8sCommand> listPruner;

    @Inject
    K8sCommandSorter commandSorter;

    @Inject
    RunnableListExecutor runnableListExecutor;

    private HelmToK8s<Xp7Deployment> helmToK8s;

    void onStartup( @Observes StartupEvent _ev )
    {
        helmToK8s =
            HelmToK8sImpl.of( k8sCommandMapper, DeploymentHelmValueBuilderImpl.of( baseValues, this::createSuPass, this::cloudApiSa ),
                              templator );
        listenToInformer( xp7DeploymentSharedIndexInformer );
    }

    @Override
    public void onAdd( final Xp7Deployment newResource )
    {
        handle( null, newResource );
    }

    @Override
    public void onUpdate( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        if ( Objects.equals( oldResource.getXp7DeploymentSpec(), newResource.getXp7DeploymentSpec() ) )
        {
            return;
        }

        handle( oldResource, newResource );
    }

    @Override
    public void onDelete( final Xp7Deployment oldResource, final boolean b )
    {
        // Do nothing
    }

    private void handle( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        helmToK8s.
            andThen( k8sCommandBuilder ).
            andThen( listPruner ).
            andThen( commandSorter ).
            andThen( runnableListExecutor ).
            apply( HelmToK8sParamsImpl.of( Optional.ofNullable( oldResource ), Optional.ofNullable( newResource ) ) );
    }

    private String createSuPass()
    {
        return UUID.randomUUID().toString().replace( "-", "" ).toLowerCase();
    }

    private ServiceAccount cloudApiSa()
    {
        return clients.k8s().serviceAccounts().
            inNamespace( cfgStr( "operator.deployment.adminServiceAccount.namespace" ) ).
            withName( cfgStr( "operator.deployment.adminServiceAccount.name" ) ).
            get();
    }
}
