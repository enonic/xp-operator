package com.enonic.ec.kubernetes.deployment;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceDoneable;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceList;

import static com.enonic.ec.kubernetes.deployment.CrdClientsProducer.createCrdClient;

@Singleton
public class XpDeploymentClientProducer
{
    private XpDeploymentClient xpDeploymentClient;

    protected XpDeploymentClientProducer( final XpDeploymentClient xpDeploymentClient )
    {
        this.xpDeploymentClient = xpDeploymentClient;
    }

    @Inject
    public XpDeploymentClientProducer( DefaultClientProducer defaultClientProducer )
    {
        xpDeploymentClient = new XpDeploymentClient(
            createCrdClient( defaultClientProducer.client(), "enonic.cloud/v1alpha1", "XPDeployment", "xp-deployments.enonic.cloud",
                             XpDeploymentResource.class, XpDeploymentResourceList.class, XpDeploymentResourceDoneable.class ) );
    }

    public XpDeploymentClient produce()
    {
        return xpDeploymentClient;
    }
}
