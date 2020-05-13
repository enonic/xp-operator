package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderSecret
    extends GenericBuilder<KubernetesClient, Secret>
{
    @Override
    protected Optional<Secret> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            secrets().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Secret resource )
    {
        return () -> client().
            secrets().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Secret resource )
    {
        return () -> client().
            secrets().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Secret resource )
    {
        return () -> client().
            secrets().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Secret o, final Secret n )
    {
        return Objects.equals( o.getData(), n.getData() );
    }
}
