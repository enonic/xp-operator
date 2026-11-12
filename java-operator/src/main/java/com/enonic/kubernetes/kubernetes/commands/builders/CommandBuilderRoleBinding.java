package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;


import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;


public class CommandBuilderRoleBinding
    extends GenericBuilder<KubernetesClient, RoleBinding>
{
    public CommandBuilderRoleBinding( final KubernetesClient client )
    {
        super( client );
    }

    @Override
    protected Optional<RoleBinding> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            rbac().
            roleBindings().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final RoleBinding resource )
    {
        return () -> client().
            rbac().
            roleBindings().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final RoleBinding resource )
    {
        return () -> client().
            rbac().
            roleBindings().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final RoleBinding resource )
    {
        return () -> client().
            rbac().
            roleBindings().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final RoleBinding o, final RoleBinding n )
    {
        return Objects.equals( o.getSubjects(), n.getSubjects() ) && Objects.equals( o.getRoleRef(), n.getRoleRef() );
    }
}
