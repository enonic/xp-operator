package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdIngresses
    extends KubeCommandBuilder<Ingress>
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
    protected void createOrReplace( final Ingress resource )
    {
        clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final Ingress resource )
    {
        clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final Ingress resource )
    {
        clients().getDefaultClient().extensions().ingresses().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Ingress o, final Ingress n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
