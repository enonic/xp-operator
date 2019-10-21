package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;

import static com.enonic.ec.kubernetes.deployment.CrdClientsProducer.createCrdClient;

@Singleton
public class IssuerClientProducer
{
    private IssuerClient issuerClient;

    @Inject
    public IssuerClientProducer( DefaultClientProducer defaultClientProducer )
    {
        issuerClient = new IssuerClient(
            createCrdClient( defaultClientProducer.client(), "apiextensions.k8s.io/v1beta1", "Issuer", "issuers.certmanager.k8s.io",
                             IssuerResource.class, IssuerResourceList.class, IssuerResourceDoneable.class ) );
    }

    public IssuerClient produce()
    {
        return issuerClient;
    }
}
