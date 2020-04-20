package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdRoleBinding
    extends KubeCommandBuilder<RoleBinding>
{
    @Override
    protected Optional<RoleBinding> fetch( final RoleBinding resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().rbac().roleBindings().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final RoleBinding resource )
    {
        clients().getDefaultClient().rbac().roleBindings().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final RoleBinding resource )
    {
        clients().getDefaultClient().rbac().roleBindings().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final RoleBinding resource )
    {
        clients().getDefaultClient().rbac().roleBindings().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final RoleBinding o, final RoleBinding n )
    {
        return Objects.equals( o.getSubjects(), n.getSubjects() ) && Objects.equals( o.getRoleRef(), n.getRoleRef() );
    }
}
