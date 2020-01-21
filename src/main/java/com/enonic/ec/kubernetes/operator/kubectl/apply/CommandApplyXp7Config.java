package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.spec.Xp7ConfigSpec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7Config
    extends CommandApplyResource<Xp7ConfigResource>
{
    protected abstract Xp7ConfigClient client();

    protected abstract Xp7ConfigSpec spec();

    @Override
    protected Optional<Xp7ConfigResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected Xp7ConfigResource build( final ObjectMeta metadata )
    {
        Xp7ConfigResource resource = new Xp7ConfigResource();
        resource.setKind( cfgStr( "operator.crd.configs.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected Xp7ConfigResource apply( final Xp7ConfigResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
