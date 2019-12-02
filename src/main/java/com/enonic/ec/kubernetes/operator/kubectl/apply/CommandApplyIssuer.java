package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.crd.issuer.IssuerResource;
import com.enonic.ec.kubernetes.crd.issuer.IssuerResourceSpec;
import com.enonic.ec.kubernetes.crd.issuer.client.IssuerClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyIssuer
    extends CommandApplyResource<IssuerResource>
{
    protected abstract IssuerClient client();

    protected abstract IssuerResourceSpec spec();

    @Override
    protected Optional<IssuerResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected IssuerResource build( final ObjectMeta metadata )
    {
        IssuerResource issuerResource = new IssuerResource();
        issuerResource.setMetadata( metadata );
        issuerResource.setSpec( spec() );
        issuerResource.setKind( cfgStr( "operator.crd.certManager.issuer.kind" ) );
        return issuerResource;
    }

    @Override
    protected IssuerResource apply( final IssuerResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
