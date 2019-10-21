package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Optional;

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
    protected Optional<IssuerResource> fetchResource()
    {
        return Optional.ofNullable( client().getClient().inNamespace( namespace().get() ).withName( name() ).get() );
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
        return client().getClient().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
