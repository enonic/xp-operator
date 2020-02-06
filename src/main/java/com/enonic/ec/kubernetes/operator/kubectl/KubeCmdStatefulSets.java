package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdStatefulSets
    extends KubeCommandBuilder<StatefulSet>
{
    @Override
    protected Optional<StatefulSet> fetch( final StatefulSet resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final StatefulSet resource )
    {
        clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void patch( final StatefulSet resource )
    {
        clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final StatefulSet resource )
    {
        clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean compareSpec( final StatefulSet o, final StatefulSet n )
    {
        // TODO: Compare Specs
        return super.compareSpec( o, n );
    }
}
