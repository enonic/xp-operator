package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderStatefulSet
    extends GenericBuilder<KubernetesClient, StatefulSet>
{
    @Override
    protected Optional<StatefulSet> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            apps().
            statefulSets().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final StatefulSet resource )
    {
        return () -> client().
            apps().
            statefulSets().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final StatefulSet resource )
    {
        return () -> client().
            apps().
            statefulSets().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final StatefulSet resource )
    {
        return () -> client().
            apps().
            statefulSets().
            inNamespace( namespace ).
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
