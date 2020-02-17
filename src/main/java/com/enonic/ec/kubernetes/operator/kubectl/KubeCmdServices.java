package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Service;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdServices
    extends KubeCommandBuilder<Service>
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
    protected void createOrReplace( final Service resource )
    {
        clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final Service resource )
    {
        clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final Service resource )
    {
        clients().getDefaultClient().services().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Service o, final Service n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
