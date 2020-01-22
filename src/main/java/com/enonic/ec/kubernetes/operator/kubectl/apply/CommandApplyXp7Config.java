package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7ConfigSpec;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7Config
    extends CommandApplyResource<V1alpha2Xp7Config>
{
    protected abstract V1alpha2Xp7ConfigSpec spec();

    @Override
    protected Optional<V1alpha2Xp7Config> fetchResource()
    {
        return Optional.ofNullable( clients().getConfigClient().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected V1alpha2Xp7Config build( final ObjectMeta metadata )
    {
        V1alpha2Xp7Config resource = new V1alpha2Xp7Config();
        resource.setKind( cfgStr( "operator.crd.configs.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected V1alpha2Xp7Config apply( final V1alpha2Xp7Config resource )
    {
        return clients().getConfigClient().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
