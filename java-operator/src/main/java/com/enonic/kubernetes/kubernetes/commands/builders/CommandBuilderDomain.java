package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.client.v1.domain.Domain;


@Value.Immutable
public abstract class CommandBuilderDomain
    extends GenericBuilder<MixedOperation<Domain, Domain.DomainList, Resource<Domain>>, Domain>
{
    @Override
    protected Optional<Domain> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Domain resource )
    {
        return () -> client().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Domain resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Domain resource )
    {
        return () -> client().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Domain o, final Domain n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
