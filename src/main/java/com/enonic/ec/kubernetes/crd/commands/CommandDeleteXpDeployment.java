package com.enonic.ec.kubernetes.crd.commands;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.deployment.client.XpDeploymentClient;

@Value.Immutable
public abstract class CommandDeleteXpDeployment
    implements Command<Boolean>
{
    public abstract XpDeploymentClient client();

    public abstract XpDeploymentResource resource();

    @Override
    public Boolean execute()
    {
        return client().client().withName( resource().getMetadata().getName() ).cascading( true ).delete();
    }
}