package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7VHost
    extends CommandApplyResource<V1alpha2Xp7VHost>
{
    protected abstract V1alpha2Xp7VHostSpec spec();

    @Override
    protected Optional<V1alpha2Xp7VHost> fetchResource()
    {
        return Optional.ofNullable( clients().getVHostClient().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected V1alpha2Xp7VHost build( final ObjectMeta metadata )
    {
        V1alpha2Xp7VHost resource = new V1alpha2Xp7VHost();
        resource.setKind( cfgStr( "operator.crd.vhosts.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected V1alpha2Xp7VHost apply( final V1alpha2Xp7VHost resource )
    {
        return clients().getVHostClient().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
