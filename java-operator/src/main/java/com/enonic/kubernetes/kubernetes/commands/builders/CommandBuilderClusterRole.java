package com.enonic.kubernetes.kubernetes.commands.builders;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;


@Value.Immutable
public abstract class CommandBuilderClusterRole
    extends GenericBuilder<KubernetesClient, ClusterRole>
{
    @Override
    protected Optional<ClusterRole> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            rbac().
            clusterRoles().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final ClusterRole resource )
    {
        return () -> client().
            rbac().
            clusterRoles().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final ClusterRole resource )
    {
        return () -> client().
            rbac().
            clusterRoles().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final ClusterRole resource )
    {
        return () -> client().
            rbac().
            clusterRoles().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ClusterRole o, final ClusterRole n )
    {
        return Objects.equals( o.getRules(), n.getRules() );
    }
}
