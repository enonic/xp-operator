package com.enonic.ec.kubernetes.operator.deployments.volumes;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;

@Value.Immutable
public abstract class VolumeTriplet
{
    public abstract List<VolumeMount> volumeMount();

    public abstract Optional<Volume> volume();

    public abstract Optional<PersistentVolumeClaim> volumeClaim();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( volume().isEmpty() ^ volumeClaim().isEmpty(), "Either volume or volumeClaim has to be set, not both" );
    }
}
