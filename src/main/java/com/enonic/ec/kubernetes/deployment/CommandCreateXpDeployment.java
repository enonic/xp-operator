package com.enonic.ec.kubernetes.deployment;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpec;

@Value.Immutable
public abstract class CommandCreateXpDeployment
    implements Command<XpDeploymentResource>
{
    private static final String kind = "XPDeployment";

    public abstract XpDeploymentClient client();

    public abstract String apiVersion();

    public abstract XpDeploymentResourceSpec spec();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( apiVersion().equals( "enonic.cloud/v1alpha1" ), "Only version 'enonic.cloud/v1alpha1' allowed" );
    }

    @Override
    public XpDeploymentResource execute()
    {
        XpDeploymentResource newDeployment = new XpDeploymentResource();
        newDeployment.setApiVersion( apiVersion() );
        newDeployment.setKind( kind );
        newDeployment.getMetadata().setName( spec().deploymentName() );
        newDeployment.getMetadata().setLabels( spec().defaultLabels() );
        newDeployment.setSpec( spec() );
        return client().client().createOrReplace( newDeployment );
    }

}
