package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderDaemonSet
    extends GenericBuilder<KubernetesClient, DaemonSet>
{
    @Override
    protected Optional<DaemonSet> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            apps().
            daemonSets().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final DaemonSet resource )
    {
        return () -> client().
            apps().
            daemonSets().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final DaemonSet resource )
    {
        return () -> client().
            apps().
            daemonSets().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final DaemonSet resource )
    {
        return () -> client().
            apps().
            daemonSets().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final DaemonSet o, final DaemonSet n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
