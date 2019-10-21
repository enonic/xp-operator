package com.enonic.ec.kubernetes.deployment;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.ImmutableXpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpec;

@Value.Immutable
public abstract class CommandCreateXpDeployment
    implements Command<XpDeploymentResource>
{
    private static final String kind = "XPDeployment";

    public abstract CrdClientsProducer.XpDeploymentClient client();

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
        XpDeploymentResource newDeployment = ImmutableXpDeploymentResource.builder().spec( spec() ).build();
        newDeployment.setApiVersion( apiVersion() );
        newDeployment.setKind( kind );
        newDeployment.getMetadata().setName( spec().deploymentName() );
        newDeployment.getMetadata().setLabels( spec().defaultLabels() );
        return client().getClient().createOrReplace( newDeployment );
    }

}
