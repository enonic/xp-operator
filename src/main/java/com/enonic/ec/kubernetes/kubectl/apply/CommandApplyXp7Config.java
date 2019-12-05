package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.config.spec.Spec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7Config
    extends CommandApplyResource<XpConfigResource>
{
    protected abstract XpConfigClient client();

    protected abstract Spec spec();

    @Override
    protected Optional<XpConfigResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected XpConfigResource build( final ObjectMeta metadata )
    {
        XpConfigResource resource = new XpConfigResource();
        resource.setKind( cfgStr( "operator.crd.xp.configs.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected XpConfigResource apply( final XpConfigResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
