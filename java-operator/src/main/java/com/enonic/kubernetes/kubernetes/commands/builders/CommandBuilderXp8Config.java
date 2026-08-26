package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.crd.v1.Xp8Config;


@Value.Immutable
public abstract class CommandBuilderXp8Config
    extends GenericBuilder<MixedOperation<Xp8Config, KubernetesResourceList<Xp8Config>, Resource<Xp8Config>>, Xp8Config>
{
    @Override
    protected Optional<Xp8Config> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp8Config resource )
    {
        return () -> client().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp8Config resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp8Config resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp8Config o, final Xp8Config n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
