package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;

@Value.Immutable
public abstract class StatefulSetSpecNonClusteredSpec
    extends StatefulSetSpecBuilder
{
    protected abstract Quantity indexDiskSize();

    protected abstract Quantity snapshotDiskSize();

    protected abstract String blobDiskPvcName();

    protected abstract String configMapName();

    private VolumeMount createVolumeMount( String pathConfigKey, String nameConfigKey )
    {
        return new VolumeMount( cfgStr( pathConfigKey ), null, cfgStr( nameConfigKey ), null, null, null );
    }

    protected List<VolumeMount> createVolumeMounts()
    {
        List<VolumeMount> volumeMounts = new LinkedList<>();
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.blob.path", "operator.deployment.xp.volume.blob.name" ) );
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.index.path", "operator.deployment.xp.volume.index.name" ) );
        volumeMounts.add(
            createVolumeMount( "operator.deployment.xp.volume.snapshots.path", "operator.deployment.xp.volume.snapshots.name" ) );
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.config.path", "operator.deployment.xp.volume.config.name" ) );
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.deploy.path", "operator.deployment.xp.volume.deploy.name" ) );
        return volumeMounts;
    }

    protected List<Volume> createVolumes()
    {
        Volume configMap = new Volume();
        configMap.setName( cfgStr( "operator.deployment.xp.volume.config.name" ) );
        configMap.setConfigMap( new ConfigMapVolumeSource( null, null, configMapName(), null ) );

        Volume deploy = new Volume();
        deploy.setName( cfgStr( "operator.deployment.xp.volume.deploy.name" ) );
        deploy.setEmptyDir( new EmptyDirVolumeSource( null, null ) );

        Volume blob = new Volume();
        blob.setName( cfgStr( "operator.deployment.xp.volume.blob.name" ) );
        blob.setPersistentVolumeClaim( new PersistentVolumeClaimVolumeSource( blobDiskPvcName(), false ) );

        return Arrays.asList( configMap, deploy, blob );
    }

    protected List<PersistentVolumeClaim> createVolumeClaimTemplates()
    {
        return Arrays.asList(
            standard( cfgStr( "operator.deployment.xp.volume.index.name" ), podLabels(), cfgStr( "operator.deployment.xp.volume.defaultStorageClass" ), "ReadWriteOnce", indexDiskSize() ),
            standard( cfgStr( "operator.deployment.xp.volume.snapshots.name" ), podLabels(), cfgStr( "operator.deployment.xp.volume.defaultStorageClass" ), "ReadWriteOnce", snapshotDiskSize() ) );
    }
}
