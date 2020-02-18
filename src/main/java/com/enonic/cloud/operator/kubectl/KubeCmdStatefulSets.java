package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


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
    protected void createOrReplace( final StatefulSet resource )
    {
        clients().getDefaultClient().apps().statefulSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
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
    protected boolean equalsSpec( final StatefulSet o, final StatefulSet n )
    {
        // Remove volume status, that is not relevant
        o.getSpec().getVolumeClaimTemplates().forEach( t -> t.setStatus( null ) );
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
