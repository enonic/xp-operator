package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;


@Value.Immutable
public abstract class CommandBuilderV1Alpha1Xp7App
    extends GenericBuilder<CrdClient, V1alpha1Xp7App>
{
    @Override
    protected Optional<V1alpha1Xp7App> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            xp7Apps().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final V1alpha1Xp7App resource )
    {
        return () -> client().
            xp7Apps().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final V1alpha1Xp7App resource )
    {
        return () -> client().
            xp7Apps().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final V1alpha1Xp7App resource )
    {
        return () -> client().
            xp7Apps().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha1Xp7App o, final V1alpha1Xp7App n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
