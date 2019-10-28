package com.enonic.ec.kubernetes.operator.commands.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyPvc
    extends CommandApplyResource<PersistentVolumeClaim>
{
    protected abstract KubernetesClient client();

    protected abstract PersistentVolumeClaimSpec spec();

    @Override
    protected Optional<PersistentVolumeClaim> fetchResource()
    {
        return Optional.ofNullable( client().persistentVolumeClaims().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected PersistentVolumeClaim build( final ObjectMeta metadata )
    {
        PersistentVolumeClaim pvc = new PersistentVolumeClaim();
        pvc.setMetadata( metadata );
        pvc.setSpec( spec() );
        return pvc;
    }

    @Override
    protected PersistentVolumeClaim apply( final PersistentVolumeClaim resource )
    {
        return client().persistentVolumeClaims().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
