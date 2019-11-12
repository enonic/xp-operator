package com.enonic.ec.kubernetes.crd.issuer.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.crd.issuer.IssuerResource;
import com.enonic.ec.kubernetes.crd.issuer.IssuerResourceDoneable;
import com.enonic.ec.kubernetes.crd.issuer.IssuerResourceList;

import static com.enonic.ec.kubernetes.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class IssuerClientProducer
{
    private final IssuerClient issuerClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public IssuerClientProducer( DefaultClientProducer defaultClientProducer,
                                 @ConfigProperty(name = "operator.crd.certManager.issuer.apiVersion") String apiVersion,
                                 @ConfigProperty(name = "operator.crd.certManager.issuer.kind") String kind,
                                 @ConfigProperty(name = "operator.crd.certManager.issuer.name") String name )
    {
        issuerClient = new IssuerClient(
            createCrdClient( defaultClientProducer.client(), apiVersion, kind, name, IssuerResource.class, IssuerResourceList.class,
                             IssuerResourceDoneable.class ) );
    }

    public IssuerClient produce()
    {
        return issuerClient;
    }
}
