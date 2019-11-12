package com.enonic.ec.kubernetes.crd.commands;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostClient;

@Value.Immutable
public abstract class CommandDeleteXpVHost
    implements Command<Boolean>
{
    protected abstract XpVHostClient client();

    public abstract XpVHostResource resource();

    @Override
    public Boolean execute()
    {
        return client().client().withName( resource().getMetadata().getName() ).cascading( true ).delete();
    }
}
