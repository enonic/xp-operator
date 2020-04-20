package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.rbac.Role;

import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdRole
    extends KubeCommandBuilder<Role>
{
    @Override
    protected Optional<Role> fetch( final Role resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().rbac().roles().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final Role resource )
    {
        clients().getDefaultClient().rbac().roles().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final Role resource )
    {
        clients().getDefaultClient().rbac().roles().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final Role resource )
    {
        clients().getDefaultClient().rbac().roles().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Role o, final Role n )
    {
        return Objects.equals( o.getRules(), n.getRules() );
    }
}
