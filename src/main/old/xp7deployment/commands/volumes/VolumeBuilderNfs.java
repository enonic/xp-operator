package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.volumes;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.NFSVolumeSource;
import io.fabric8.kubernetes.api.model.Volume;

@Value.Immutable
public abstract class VolumeBuilderNfs
    extends VolumeBuilder
{
    protected abstract String nfsServer();

    protected abstract String nfsPath();

    @Override
    protected Volume sharedVolume( final String name )
    {
        Volume volume = new Volume();
        volume.setName( name );
        volume.setNfs( new NFSVolumeSource( nfsPath(), false, nfsServer() ) );
        return volume;
    }
}
