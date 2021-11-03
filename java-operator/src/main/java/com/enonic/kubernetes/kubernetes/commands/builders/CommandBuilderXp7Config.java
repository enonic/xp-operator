package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;


@Value.Immutable
public abstract class CommandBuilderXp7Config
    extends GenericBuilder<MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>>, Xp7Config>
{
    @Override
    protected Optional<Xp7Config> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp7Config resource )
    {
        return () -> client().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp7Config resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp7Config resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp7Config o, final Xp7Config n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
