package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Optional;


import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.crd.v1.Xp8Config;

import static com.enonic.kubernetes.kubernetes.Predicates.valueEquals;

public class CommandBuilderXp8Config
    extends GenericBuilder<MixedOperation<Xp8Config, KubernetesResourceList<Xp8Config>, Resource<Xp8Config>>, Xp8Config>
{
    public CommandBuilderXp8Config( final MixedOperation<Xp8Config, KubernetesResourceList<Xp8Config>, Resource<Xp8Config>> client )
    {
        super( client );
    }

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
        return valueEquals( o.getSpec(), n.getSpec() );
    }
}
