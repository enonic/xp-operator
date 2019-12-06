package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostClient;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.Spec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7VHost
    extends CommandApplyResource<XpVHostResource>
{
    protected abstract XpVHostClient client();

    protected abstract Spec spec();

    @Override
    protected Optional<XpVHostResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected XpVHostResource build( final ObjectMeta metadata )
    {
        XpVHostResource resource = new XpVHostResource();
        resource.setKind( cfgStr( "operator.crd.xp.vhosts.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected XpVHostResource apply( final XpVHostResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
