package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withMetadata( resource.getMetadata() ).
            done();

        // Everything is immutable after creation except resources.requests
        if(resource.getSpec().getResources().getRequests() != null) {
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
    }

    @Override
    protected void delete( final PersistentVolumeClaim resource )
    {
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
