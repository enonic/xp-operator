package com.enonic.kubernetes.kubernetes.commands.builders;

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
        return Optional.ofNullable( client().
            rbac().
            clusterRoleBindings().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final ClusterRoleBinding resource )
    {
        return () -> client().
            rbac().
            clusterRoleBindings().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final ClusterRoleBinding resource )
    {
        return () -> client().
            rbac().
            clusterRoleBindings().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final ClusterRoleBinding resource )
    {
        return () -> client().
            rbac().
            clusterRoleBindings().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ClusterRoleBinding o, final ClusterRoleBinding n )
    {
        return Objects.equals( o.getSubjects(), n.getSubjects() ) && Objects.equals( o.getRoleRef(), n.getRoleRef() );
    }
}
