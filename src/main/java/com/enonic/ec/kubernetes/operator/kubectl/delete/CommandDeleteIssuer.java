package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.issuer.client.IssuerClient;

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
