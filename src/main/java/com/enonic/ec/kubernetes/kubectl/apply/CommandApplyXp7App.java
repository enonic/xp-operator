package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppClient;
import com.enonic.ec.kubernetes.operator.crd.app.spec.Spec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7App
    extends CommandApplyResource<XpAppResource>
{
    protected abstract XpAppClient client();

    protected abstract Spec spec();

    @Override
    protected Optional<XpAppResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected XpAppResource build( final ObjectMeta metadata )
    {
        XpAppResource resource = new XpAppResource();
        resource.setKind( cfgStr( "operator.crd.xp.apps.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected XpAppResource apply( final XpAppResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
