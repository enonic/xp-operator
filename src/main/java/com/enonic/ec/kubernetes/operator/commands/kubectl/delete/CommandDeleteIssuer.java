package com.enonic.ec.kubernetes.operator.commands.kubectl.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

@Value.Immutable
public abstract class CommandDeleteIssuer
    extends CommandDeleteResource
{
    protected abstract IssuerClient client();

    @Override
    protected String resourceKind()
    {
        return "Issuer";
    }

    @Override
    protected Boolean delete()
    {
        return client().client().inNamespace( namespace() ).withName( name() ).delete();
    }
}
