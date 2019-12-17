package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.client.Xp7VHostClient;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.Xp7VHostSpec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7VHost
    extends CommandApplyResource<Xp7VHostResource>
{
    protected abstract Xp7VHostClient client();

    protected abstract Xp7VHostSpec spec();

    @Override
    protected Optional<Xp7VHostResource> fetchResource()
    {
        return Optional.ofNullable( client().client().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected Xp7VHostResource build( final ObjectMeta metadata )
    {
        Xp7VHostResource resource = new Xp7VHostResource();
        resource.setKind( cfgStr( "operator.crd.xp.vhosts.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected Xp7VHostResource apply( final Xp7VHostResource resource )
    {
        return client().client().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
