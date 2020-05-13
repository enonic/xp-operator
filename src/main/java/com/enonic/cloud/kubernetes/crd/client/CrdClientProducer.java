package com.enonic.cloud.kubernetes.crd.client;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.client.V1alpha1Xp7AppDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.client.V1alpha1Xp7AppList;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.client.V1alpha2Xp7ConfigDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.client.V1alpha2Xp7ConfigList;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.client.V1alpha2Xp7DeploymentDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.client.V1alpha2Xp7DeploymentList;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.client.V1alpha2Xp7VHostDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.client.V1alpha2Xp7VHostList;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class CrdClientProducer
{
    @Singleton
    @Produces
    CrdClient crdClient( final KubernetesClient client )
    {
        return CrdClientImpl.of(
            createCrdClient( client, cfgStr( "operator.crd.apps.name" ), V1alpha1Xp7App.class, V1alpha1Xp7AppList.class,
                             V1alpha1Xp7AppDoneable.class ),
            createCrdClient( client, cfgStr( "operator.crd.configs.name" ), V1alpha2Xp7Config.class, V1alpha2Xp7ConfigList.class,
                             V1alpha2Xp7ConfigDoneable.class ),
            createCrdClient( client, cfgStr( "operator.crd.deployments.name" ), V1alpha2Xp7Deployment.class,
                             V1alpha2Xp7DeploymentList.class, V1alpha2Xp7DeploymentDoneable.class ),
            createCrdClient( client, cfgStr( "operator.crd.vhosts.name" ), V1alpha2Xp7VHost.class, V1alpha2Xp7VHostList.class,
                             V1alpha2Xp7VHostDoneable.class ) );
    }

    private <T extends HasMetadata, L extends CustomResourceList<T>, D extends Doneable<T>> MixedOperation<T, L, D, Resource<T, D>> createCrdClient(
        final KubernetesClient client, final String crdName, final Class<T> resourceClass, final Class<L> resourceListClass,
        final Class<D> resourceDoneableClass )
    {
        return client.customResources( getCRD( client, crdName ), resourceClass, resourceListClass, resourceDoneableClass );
    }

    private CustomResourceDefinition getCRD( final KubernetesClient client, final String name )
    {
        return client.customResourceDefinitions().list().getItems().stream().
            filter( d -> name.equals( d.getMetadata().getName() ) ).
            findAny().
            orElseThrow( () -> new RuntimeException( "Deployment error: Custom resource definition with name '" + name + "' not found." ) );
    }
}
