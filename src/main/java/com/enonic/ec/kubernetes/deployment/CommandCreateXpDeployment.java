package com.enonic.ec.kubernetes.deployment;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpec;

@Value.Immutable
public abstract class CommandCreateXpDeployment
    extends Configuration
    implements Command<XpDeploymentResource>
{
    public abstract XpDeploymentClient client();

    public abstract String apiVersion();

    public abstract XpDeploymentResourceSpec spec();

    @Value.Check
    protected void check()
    {
        String operatorApiVersion = cfgStr( "operator.crd.xp.apiVersion" );
        Preconditions.checkState( apiVersion().equals( operatorApiVersion ), "Only version '" + operatorApiVersion + "' allowed" );
    }

    @Override
    public XpDeploymentResource execute()
    {
        XpDeploymentResource newDeployment = new XpDeploymentResource();
        newDeployment.setApiVersion( apiVersion() );
        newDeployment.setKind( cfgStr( "operator.crd.xp.kind" ) );
        newDeployment.getMetadata().setName( spec().deploymentName() );
        newDeployment.getMetadata().setLabels( spec().defaultLabels() );
        newDeployment.setSpec( spec() );
        return client().client().createOrReplace( newDeployment );
    }
}
