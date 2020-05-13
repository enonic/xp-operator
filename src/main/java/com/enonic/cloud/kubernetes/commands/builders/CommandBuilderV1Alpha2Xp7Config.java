package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;


@Value.Immutable
public abstract class CommandBuilderV1Alpha2Xp7Config
    extends GenericBuilder<CrdClient, V1alpha2Xp7Config>
{
    @Override
    protected Optional<V1alpha2Xp7Config> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            xp7Configs().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final V1alpha2Xp7Config resource )
    {
        return () -> client().
            xp7Configs().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final V1alpha2Xp7Config resource )
    {
        return () -> client().
            xp7Configs().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final V1alpha2Xp7Config resource )
    {
        return () -> client().
            xp7Configs().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha2Xp7Config o, final V1alpha2Xp7Config n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
