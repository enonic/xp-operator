package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResource;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResourceSpec;

@Value.Immutable
public abstract class CommandApplyIssuer
    extends CommandApplyResource<IssuerResource>
{
    private static final String kind = "Issuer";

    protected abstract IssuerClientProducer.IssuerClient client();

    protected abstract IssuerResourceSpec spec();

    @Override
    protected IssuerResource fetchResource()
    {
        return client().getClient().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected IssuerResource build( final ObjectMeta metadata )
    {
        IssuerResource issuerResource = new IssuerResource();
        issuerResource.setMetadata( metadata );
        issuerResource.setSpec( spec() );
        issuerResource.setKind( kind );
        return issuerResource;
    }

    @Override
    protected IssuerResource apply( final IssuerResource resource )
    {
        return client().getClient().inNamespace( namespace() ).createOrReplace( resource );
    }
}
