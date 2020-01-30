package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdStatefulSets
    extends KubeCommandResource<StatefulSet>
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
    protected void update( final StatefulSet resource )
    {
        clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final StatefulSet resource )
    {
        clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
