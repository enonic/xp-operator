package com.enonic.kubernetes.kubernetes.commands.builders;

import com.google.common.base.Preconditions;
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
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRole" );
        return Optional.ofNullable( client().
            rbac().
            clusterRoles().
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final ClusterRole resource )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRole" );
        return () -> client().
            rbac().
            clusterRoles().
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final ClusterRole resource )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRole" );
        return () -> client().
            rbac().
            clusterRoles().
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final ClusterRole resource )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRole" );
        return () -> client().
            rbac().
            clusterRoles().
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ClusterRole o, final ClusterRole n )
    {
        return Objects.equals( o.getRules(), n.getRules() );
    }
}
