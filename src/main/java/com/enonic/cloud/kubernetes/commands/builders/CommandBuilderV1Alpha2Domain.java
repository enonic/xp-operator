package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.client.v1alpha2.domain.DomainClient;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;


@Value.Immutable
public abstract class CommandBuilderV1Alpha2Domain
    extends GenericBuilder<DomainClient, Domain>
{
    @Override
    protected Optional<Domain> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().crdClient().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Domain resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Domain resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Domain resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Domain o, final Domain n )
    {
        return Objects.equals( o.getDomainSpec(), n.getDomainSpec() );
    }
}
