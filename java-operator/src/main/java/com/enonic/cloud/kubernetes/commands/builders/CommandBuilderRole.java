package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderRole
    extends GenericBuilder<KubernetesClient, Role>
{
    @Override
    protected Optional<Role> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            rbac().
            roles().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Role resource )
    {
        return () -> client().
            rbac().
            roles().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Role resource )
    {
        return () -> client().
            rbac().
            roles().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Role resource )
    {
        return () -> client().
            rbac().
            roles().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Role o, final Role n )
    {
        return Objects.equals( o.getRules(), n.getRules() );
    }
}
