package com.enonic.ec.kubernetes.deployment.commands;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.XpDeploymentClient;
import com.enonic.ec.kubernetes.deployment.XpDeploymentResource;

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
