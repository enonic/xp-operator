package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import static com.enonic.ec.kubernetes.deployment.CrdClientsProducer.createCrdClient;

public class IssuerClientProducer
{
    public static class IssuerClient
    {

        private final MixedOperation<IssuerResource, IssuerResourceList, IssuerResourceDoneable, Resource<IssuerResource, IssuerResourceDoneable>>
            client;

        public IssuerClient(
            final MixedOperation<IssuerResource, IssuerResourceList, IssuerResourceDoneable, Resource<IssuerResource, IssuerResourceDoneable>> client )
        {
            this.client = client;
        }

        public MixedOperation<IssuerResource, IssuerResourceList, IssuerResourceDoneable, Resource<IssuerResource, IssuerResourceDoneable>> getClient()
        {
            return client;
        }
    }

    @Produces
    @Singleton
    IssuerClient produceXpDeploymentClient( @Named("default") KubernetesClient defaultClient )
    {
        return new IssuerClient(
            createCrdClient( defaultClient, "certmanager.k8s.io/v1alpha1", "Issuer", "issuers.cert-manager.io", IssuerResource.class,
                             IssuerResourceList.class, IssuerResourceDoneable.class ) );
    }
}
