package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderPVC
    extends GenericBuilder<KubernetesClient, PersistentVolumeClaim>
{
    @Override
    protected Optional<PersistentVolumeClaim> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            persistentVolumeClaims().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final PersistentVolumeClaim resource )
    {
        return () -> client().
            persistentVolumeClaims().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final PersistentVolumeClaim resource )
    {
        return () -> client().
            persistentVolumeClaims().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final PersistentVolumeClaim resource )
    {
        return () -> client().
            persistentVolumeClaims().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalAnnotations( final Map<String, String> o, final Map<String, String> n )
    {
        // Ignore annotations on PVCs
        return true;
    }

    @Override
    protected boolean equalsSpec( final PersistentVolumeClaim o, final PersistentVolumeClaim n )
    {
        return Objects.equals( o.getSpec().getResources().getRequests(), n.getSpec().getResources().getRequests() );
    }
}
