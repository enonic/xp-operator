package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Optional;


import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;

import static com.enonic.kubernetes.kubernetes.Predicates.valueEquals;

public class CommandBuilderXp8Deployment
    extends GenericBuilder<MixedOperation<Xp8Deployment, KubernetesResourceList<Xp8Deployment>, Resource<Xp8Deployment>>, Xp8Deployment>
{
    public CommandBuilderXp8Deployment( final MixedOperation<Xp8Deployment, KubernetesResourceList<Xp8Deployment>, Resource<Xp8Deployment>> client )
    {
        super( client );
    }

    @Override
    protected Optional<Xp8Deployment> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp8Deployment resource )
    {
        return () -> client().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp8Deployment resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp8Deployment resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp8Deployment o, final Xp8Deployment n )
    {
        return valueEquals( o.getSpec(), n.getSpec() );
    }
}
