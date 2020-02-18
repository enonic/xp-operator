package com.enonic.cloud.operator.kubectl;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;

import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdPVCs
    extends KubeCommandBuilder<PersistentVolumeClaim>
{
    @Override
    protected Optional<PersistentVolumeClaim> fetch( final PersistentVolumeClaim resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final PersistentVolumeClaim resource )
    {
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final PersistentVolumeClaim resource )
    {
        // Update metadata
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withMetadata( resource.getMetadata() ).
            done();

        // Everything in spec is immutable after creation except resources.requests
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            editSpec().
            editOrNewResources().
            withRequests( resource.getSpec().getResources().getRequests() ).
            endResources().
            endSpec().
            done();
    }

    @Override
    protected void delete( final PersistentVolumeClaim resource )
    {
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
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
