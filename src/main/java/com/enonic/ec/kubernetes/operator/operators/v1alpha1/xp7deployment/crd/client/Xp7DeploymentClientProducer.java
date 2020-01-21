package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.operator.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResourceDoneable;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class Xp7DeploymentClientProducer
{
    private final Xp7DeploymentClient xp7DeploymentClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public Xp7DeploymentClientProducer( DefaultClientProducer defaultClientProducer,
                                        @ConfigProperty(name = "operator.crd.v1alpha1.group") String group,
                                        @ConfigProperty(name = "operator.crd.v1alpha1.apiVersion") String apiVersion,
                                        @ConfigProperty(name = "operator.crd.v1alpha1.deployments.kind") String kind,
                                        @ConfigProperty(name = "operator.crd.v1alpha1.deployments.name") String name )
    {
        xp7DeploymentClient = new Xp7DeploymentClient(
            createCrdClient( defaultClientProducer.client(), group + "/" + apiVersion, kind, name, Xp7DeploymentResource.class,
                             Xp7DeploymentResourceList.class, Xp7DeploymentResourceDoneable.class ) );
    }

    public Xp7DeploymentClient produce()
    {
        return xp7DeploymentClient;
    }
}
