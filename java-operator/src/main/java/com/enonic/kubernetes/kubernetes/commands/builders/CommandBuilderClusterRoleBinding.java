package com.enonic.kubernetes.kubernetes.commands.builders;

import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;


@Value.Immutable
public abstract class CommandBuilderClusterRoleBinding
    extends GenericBuilder<KubernetesClient, ClusterRoleBinding>
{
    @Override
    protected Optional<ClusterRoleBinding> getOldResource( final String namespace, final String name )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRoleBinding" );
        return Optional.ofNullable( client().
            rbac().
            clusterRoleBindings().
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final ClusterRoleBinding resource )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRoleBinding" );
        return () -> client().
            rbac().
            clusterRoleBindings().
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final ClusterRoleBinding resource )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRoleBinding" );
        return () -> client().
            rbac().
            clusterRoleBindings().
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final ClusterRoleBinding resource )
    {
        Preconditions.checkArgument( namespace == null, "Cannot assign namespace to ClusterRoleBinding" );
        return () -> client().
            rbac().
            clusterRoleBindings().
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ClusterRoleBinding o, final ClusterRoleBinding n )
    {
        return Objects.equals( o.getSubjects(), n.getSubjects() ) && Objects.equals( o.getRoleRef(), n.getRoleRef() );
    }
}
