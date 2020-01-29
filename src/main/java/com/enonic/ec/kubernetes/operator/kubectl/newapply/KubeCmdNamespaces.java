package com.enonic.ec.kubernetes.operator.kubectl.newapply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Namespace;

import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdNamespaces
    extends KubeCommandResource<Namespace>
{
    @Override
    protected Optional<Namespace> fetch( final Namespace resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().namespaces().
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final Namespace resource )
    {
        clients().getDefaultClient().namespaces().
            create( resource );
    }

    @Override
    protected void update( final Namespace resource )
    {
        clients().getDefaultClient().namespaces().
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final Namespace resource )
    {
        clients().getDefaultClient().namespaces().
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
