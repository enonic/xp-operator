package com.enonic.ec.kubernetes.deployment;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;

import static com.enonic.ec.kubernetes.deployment.CrdClientsProducer.createCrdClient;

@Singleton
public class XpDeploymentClientProducer
{
    private final XpDeploymentClient xpDeploymentClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public XpDeploymentClientProducer( DefaultClientProducer defaultClientProducer,
                                       @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                       @ConfigProperty(name = "operator.crd.xp.kind") String kind,
                                       @ConfigProperty(name = "operator.crd.xp.name") String name )
    {
        xpDeploymentClient = new XpDeploymentClient(
            createCrdClient( defaultClientProducer.client(), apiVersion, kind, name, XpDeploymentResource.class,
                             XpDeploymentResourceList.class, XpDeploymentResourceDoneable.class ) );
    }

    public XpDeploymentClient produce()
    {
        return xpDeploymentClient;
    }
}
