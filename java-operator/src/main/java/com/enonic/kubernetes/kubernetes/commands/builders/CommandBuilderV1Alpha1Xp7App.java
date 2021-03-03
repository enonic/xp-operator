package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;


@Value.Immutable
public abstract class CommandBuilderV1Alpha1Xp7App
    extends GenericBuilder<MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>>, Xp7App>
{
    @Override
    protected Optional<Xp7App> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp7App resource )
    {
        return () -> client().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp7App resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp7App resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp7App o, final Xp7App n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
