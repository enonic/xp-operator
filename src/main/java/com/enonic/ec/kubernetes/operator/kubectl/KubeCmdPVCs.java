package com.enonic.ec.kubernetes.operator.kubectl;

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
    protected void update( final PersistentVolumeClaim resource )
    {
        clients().getDefaultClient().persistentVolumeClaims().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
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
