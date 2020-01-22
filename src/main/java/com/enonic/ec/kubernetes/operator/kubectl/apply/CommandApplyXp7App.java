package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7AppSpec;

;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyXp7App
    extends CommandApplyResource<V1alpha1Xp7App>
{
    protected abstract V1alpha1Xp7AppSpec spec();

    @Override
    protected Optional<V1alpha1Xp7App> fetchResource()
    {
        return Optional.ofNullable( clients().getAppClient().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected V1alpha1Xp7App build( final ObjectMeta metadata )
    {
        V1alpha1Xp7App resource = new V1alpha1Xp7App();
        resource.setKind( cfgStr( "operator.crd.apps.kind" ) );
        resource.setMetadata( metadata );
        resource.setSpec( spec() );
        return resource;
    }

    @Override
    protected V1alpha1Xp7App apply( final V1alpha1Xp7App resource )
    {
        return clients().getAppClient().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
