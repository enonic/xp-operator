package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Service;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdServices
    extends KubeCommandResource<Service>
{
    @Override
    protected Optional<Service> fetch( final Service resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final Service resource )
    {
        clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void update( final Service resource )
    {
        clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final Service resource )
    {
        clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
