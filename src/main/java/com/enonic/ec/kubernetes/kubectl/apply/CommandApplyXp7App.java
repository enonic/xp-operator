package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7app.client.Xp7AppClient;
import com.enonic.ec.kubernetes.operator.crd.xp7app.spec.Xp7AppSpec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7App
    extends CommandApplyResource<Xp7AppResource>
{
    protected abstract Xp7AppClient client();

    protected abstract Xp7AppSpec spec();

    @Override
    protected Optional<Xp7AppResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected Xp7AppResource build( final ObjectMeta metadata )
    {
        Xp7AppResource resource = new Xp7AppResource();
        resource.setKind( cfgStr( "operator.crd.xp.apps.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected Xp7AppResource apply( final Xp7AppResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
