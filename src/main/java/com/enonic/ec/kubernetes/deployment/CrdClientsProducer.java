package com.enonic.ec.kubernetes.deployment;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResourceDoneable;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResourceList;

public class CrdClientsProducer
{

    private static <T extends HasMetadata, L extends CustomResourceList<T>, D extends Doneable<T>> MixedOperation<T, L, D, Resource<T, D>> createCrdClient(
        final KubernetesClient defaultClient, final String apiVersion, final String kind, final String name, final Class<T> resourceClass,
        final Class<L> resourceListClass, final Class<D> resourceDoneableClass )
    {
        KubernetesDeserializer.registerCustomKind( apiVersion, kind, resourceClass );
        CustomResourceDefinition crd = defaultClient.customResourceDefinitions().list().getItems().stream().filter(
            d -> name.equals( d.getMetadata().getName() ) ).findAny().orElseThrow( () -> new RuntimeException(
            "Deployment error: Custom resource definition of kind '" + kind + "' with name '" + name + "' not found." ) );

        return defaultClient.customResources( crd, resourceClass, resourceListClass, resourceDoneableClass );
    }

    public static class XpDeploymentClient
    {

        private final MixedOperation<XpDeploymentResource, XpDeploymentResourceList, XpDeploymentResourceDoneable, Resource<XpDeploymentResource, XpDeploymentResourceDoneable>>
            client;

        public XpDeploymentClient(
            final MixedOperation<XpDeploymentResource, XpDeploymentResourceList, XpDeploymentResourceDoneable, Resource<XpDeploymentResource, XpDeploymentResourceDoneable>> client )
        {
            this.client = client;
        }

        public MixedOperation<XpDeploymentResource, XpDeploymentResourceList, XpDeploymentResourceDoneable, Resource<XpDeploymentResource, XpDeploymentResourceDoneable>> getClient()
        {
            return client;
        }
    }

    @Produces
    @Singleton
    XpDeploymentClient produceXpDeploymentClient( @Named("default") KubernetesClient defaultClient )
    {
        return new XpDeploymentClient(
            createCrdClient( defaultClient, "enonic.cloud/v1alpha1", "XPDeployment", "xp-deployments.enonic.cloud",
                             XpDeploymentResource.class, XpDeploymentResourceList.class, XpDeploymentResourceDoneable.class ) );
    }
}
