package com.enonic.ec.kubernetes.crd;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class CrdClientsProducer
{
    public static <T extends HasMetadata, L extends CustomResourceList<T>, D extends Doneable<T>> MixedOperation<T, L, D, Resource<T, D>> createCrdClient(
        final KubernetesClient defaultClient, final String apiVersion, final String kind, final String name, final Class<T> resourceClass,
        final Class<L> resourceListClass, final Class<D> resourceDoneableClass )
    {
        KubernetesDeserializer.registerCustomKind( apiVersion, kind, resourceClass );
        CustomResourceDefinition crd = defaultClient.customResourceDefinitions().list().getItems().stream().filter(
            d -> name.equals( d.getMetadata().getName() ) ).findAny().orElseThrow( () -> new RuntimeException(
            "Deployment error: Custom resource definition of kind '" + kind + "' with name '" + name + "' not found." ) );

        return defaultClient.customResources( crd, resourceClass, resourceListClass, resourceDoneableClass );
    }
}
