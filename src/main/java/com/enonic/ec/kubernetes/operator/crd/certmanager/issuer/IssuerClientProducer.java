package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;

import static com.enonic.ec.kubernetes.deployment.CrdClientsProducer.createCrdClient;

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
