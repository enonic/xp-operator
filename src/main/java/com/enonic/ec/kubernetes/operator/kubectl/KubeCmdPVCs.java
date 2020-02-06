package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdPVCs
    extends KubeCommandResource<PersistentVolumeClaim>
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
    protected void create( final PersistentVolumeClaim resource )
    {
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
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
    protected boolean compareSpec( final PersistentVolumeClaim o, final PersistentVolumeClaim n )
    {
        return Objects.equals( o.getSpec().getResources().getRequests(), n.getSpec().getResources().getRequests() );
    }
}
