package com.enonic.ec.kubernetes.operator.deployments.volumes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Volume;

@Value.Immutable
public abstract class VolumeBuilderStd
    extends VolumeBuilder
{
    protected abstract String sharedStoragePVCName();

    @Override
    protected Volume sharedVolume( final String name )
    {
        Volume volume = new Volume();
        volume.setName( name );
        volume.setPersistentVolumeClaim( new PersistentVolumeClaimVolumeSource( sharedStoragePVCName(), false ) );
        return volume;
    }

}
