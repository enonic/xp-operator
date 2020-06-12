package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppClient;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;


@Value.Immutable
public abstract class CommandBuilderV1Alpha1Xp7App
    extends GenericBuilder<Xp7AppClient, Xp7App>
{
    @Override
    protected Optional<Xp7App> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().crdClient().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp7App resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp7App resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp7App resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp7App o, final Xp7App n )
    {
        return Objects.equals( o.getXp7AppSpec(), n.getXp7AppSpec() );
    }
}
