package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdIngresses
    extends KubeCommandResource<Ingress>
{
    @Override
    protected Optional<Ingress> fetch( final Ingress resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final Ingress resource )
    {
        clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void update( final Ingress resource )
    {
        clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final Ingress resource )
    {
        clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
