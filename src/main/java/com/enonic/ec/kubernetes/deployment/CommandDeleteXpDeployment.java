package com.enonic.ec.kubernetes.deployment;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;

@Value.Immutable
public abstract class CommandDeleteXpDeployment
    implements Command<Boolean>
{
    public abstract CrdClientsProducer.XpDeploymentClient client();

    public abstract XpDeploymentResource resource();

    @Override
    public Boolean execute()
    {
        return client().getClient().withName( resource().getMetadata().getName() ).cascading( true ).delete();
    }
}
