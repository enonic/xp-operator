package com.enonic.ec.kubernetes.operator.operators.common.clients;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;

@Singleton
public class Clients
{
    private final MixedOperation<V1alpha2Xp7VHost, V1alpha2Xp7VHostList, V1alpha2Xp7VHostDoneable, Resource<V1alpha2Xp7VHost, V1alpha2Xp7VHostDoneable>>
        vHostClient;

    private final KubernetesClient defaultClient;

    private final MixedOperation<V1alpha1Xp7App, V1alpha1Xp7AppList, V1alpha1Xp7AppDoneable, Resource<V1alpha1Xp7App, V1alpha1Xp7AppDoneable>>
        appClient;

    private final MixedOperation<V1alpha2Xp7Config, V1alpha2Xp7ConfigList, V1alpha2Xp7ConfigDoneable, Resource<V1alpha2Xp7Config, V1alpha2Xp7ConfigDoneable>>
        configClient;

    private final MixedOperation<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList, V1alpha2Xp7DeploymentDoneable, Resource<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentDoneable>>
        deploymentClient;

    @Inject
    public Clients( DefaultClientProducer defaultClientProducer )
    {
        defaultClient = defaultClientProducer.client();
        appClient = createCrdClient( cfgStr( "operator.crd.apps.name" ), V1alpha1Xp7App.class, V1alpha1Xp7AppList.class,
                                     V1alpha1Xp7AppDoneable.class );
        configClient = createCrdClient( cfgStr( "operator.crd.configs.name" ), V1alpha2Xp7Config.class, V1alpha2Xp7ConfigList.class,
                                        V1alpha2Xp7ConfigDoneable.class );
        deploymentClient =
            createCrdClient( cfgStr( "operator.crd.deployments.name" ), V1alpha2Xp7Deployment.class, V1alpha2Xp7DeploymentList.class,
                             V1alpha2Xp7DeploymentDoneable.class );
        vHostClient = createCrdClient( cfgStr( "operator.crd.vhosts.name" ), V1alpha2Xp7VHost.class, V1alpha2Xp7VHostList.class,
                                       V1alpha2Xp7VHostDoneable.class );
    }

    private CustomResourceDefinition getCRD( String name )
    {
        return defaultClient.customResourceDefinitions().list().getItems().stream().
            filter( d -> name.equals( d.getMetadata().getName() ) ).
            findAny().
            orElseThrow( () -> new RuntimeException( "Deployment error: Custom resource definition with name '" + name + "' not found." ) );
    }

    private <T extends HasMetadata, L extends CustomResourceList<T>, D extends Doneable<T>> MixedOperation<T, L, D, Resource<T, D>> createCrdClient(
        final String crdName, final Class<T> resourceClass, final Class<L> resourceListClass, final Class<D> resourceDoneableClass )
    {
        return defaultClient.customResources( getCRD( crdName ), resourceClass, resourceListClass, resourceDoneableClass );
    }

    public KubernetesClient getDefaultClient()
    {
        return defaultClient;
    }

    public MixedOperation<V1alpha1Xp7App, V1alpha1Xp7AppList, V1alpha1Xp7AppDoneable, Resource<V1alpha1Xp7App, V1alpha1Xp7AppDoneable>> getAppClient()
    {
        return appClient;
    }

    public MixedOperation<V1alpha2Xp7Config, V1alpha2Xp7ConfigList, V1alpha2Xp7ConfigDoneable, Resource<V1alpha2Xp7Config, V1alpha2Xp7ConfigDoneable>> getConfigClient()
    {
        return configClient;
    }

    public MixedOperation<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList, V1alpha2Xp7DeploymentDoneable, Resource<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentDoneable>> getDeploymentClient()
    {
        return deploymentClient;
    }

    public MixedOperation<V1alpha2Xp7VHost, V1alpha2Xp7VHostList, V1alpha2Xp7VHostDoneable, Resource<V1alpha2Xp7VHost, V1alpha2Xp7VHostDoneable>> getVHostClient()
    {
        return vHostClient;
    }
}
