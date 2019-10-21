package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

@Value.Immutable
public abstract class CommandDeleteIssuer
    extends CommandDeleteResource
{
    protected abstract IssuerClientProducer.IssuerClient client();

    @Override
    protected String resourceKind()
    {
        return "Issuer";
    }

    @Override
    protected Boolean delete()
    {
        return client().getClient().inNamespace( namespace() ).withName( name() ).delete();
    }
}
