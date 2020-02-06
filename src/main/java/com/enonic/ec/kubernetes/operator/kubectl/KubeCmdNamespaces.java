package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Namespace;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdNamespaces
    extends KubeCommandBuilder<Namespace>
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
    protected void patch( final Namespace resource )
    {
        clients().getDefaultClient().namespaces().
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final Namespace resource )
    {
        clients().getDefaultClient().namespaces().
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean compareSpec( final Namespace o, final Namespace n )
    {
        // If new namespace spec is empty, there is no update to the namespace
        return n.getSpec() == null;
    }
}
