package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;

@Value.Immutable
public abstract class StatefulSetSpecNonClusteredSpec
    extends StatefulSetSpecBuilder
{
    protected abstract Quantity repoDiskSize();

    protected abstract Quantity snapshotDiskSize();

    protected List<VolumeMount> createVolumeMounts()
    {
        List<VolumeMount> volumeMounts = new LinkedList<>();
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/repo", null, volumeRepo, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/snapshots", null, volumeSnapshots, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/config", null, volumeConfig, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/deploy", null, volumeDeploy, null, null, null ) );
        return volumeMounts;
    }

    protected List<Volume> createVolumes()
    {
        Volume configMap = new Volume();
        configMap.setName( volumeConfig );
        configMap.setConfigMap( new ConfigMapVolumeSource( null, null, configMapName(), null ) );

        Volume deploy = new Volume();
        deploy.setName( volumeDeploy );
        deploy.setEmptyDir( new EmptyDirVolumeSource( null, null ) );

        return Arrays.asList( configMap, deploy );
    }

    protected List<PersistentVolumeClaim> createVolumeClaimTemplates()
    {
        return Arrays.asList( standard( volumeRepo, podLabels(), null, "ReadWriteOnce", repoDiskSize() ),
                              standard( volumeSnapshots, podLabels(), null, "ReadWriteOnce", snapshotDiskSize() ) );
    }
}
