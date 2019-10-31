package com.enonic.ec.kubernetes.operator.commands.builders.spec;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;

@Value.Immutable
public abstract class StatefulSetSpecVolumes
    extends StatefulSetSpecBase
{
    protected abstract String blobDiskPvcName();

    protected abstract String configMapName();

    protected abstract Optional<Quantity> indexDiskSize();

    protected abstract Optional<String> snapshotsPvcName();

    private VolumeMount createVolumeMount( String pathConfigKey, String nameConfigKey )
    {
        return new VolumeMount( cfgStr( pathConfigKey ), null, cfgStr( nameConfigKey ), null, null, null );
    }

    protected List<VolumeMount> createVolumeMounts()
    {
        List<VolumeMount> volumeMounts = new LinkedList<>();
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.blob.path", "operator.deployment.xp.volume.blob.name" ) );
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.config.path", "operator.deployment.xp.volume.config.name" ) );
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.deploy.path", "operator.deployment.xp.volume.deploy.name" ) );
        volumeMounts.add( createVolumeMount( "operator.deployment.xp.volume.index.path", "operator.deployment.xp.volume.index.name" ) );

        if ( snapshotsPvcName().isPresent() )
        {
            volumeMounts.add(
                createVolumeMount( "operator.deployment.xp.volume.snapshots.path", "operator.deployment.xp.volume.snapshots.name" ) );
        }

        return volumeMounts;
    }

    protected List<Volume> createVolumes()
    {
        List<Volume> res = new LinkedList<>();

        Volume blob = new Volume();
        blob.setName( cfgStr( "operator.deployment.xp.volume.blob.name" ) );
        blob.setPersistentVolumeClaim( new PersistentVolumeClaimVolumeSource( blobDiskPvcName(), false ) );
        res.add( blob );

        Volume configMap = new Volume();
        configMap.setName( cfgStr( "operator.deployment.xp.volume.config.name" ) );
        configMap.setConfigMap( new ConfigMapVolumeSource( null, null, configMapName(), null ) );
        res.add( configMap );

        Volume deploy = new Volume();
        deploy.setName( cfgStr( "operator.deployment.xp.volume.deploy.name" ) );
        deploy.setEmptyDir( new EmptyDirVolumeSource( null, null ) );
        res.add( deploy );

        if ( indexDiskSize().isEmpty() )
        { // Index is not persisted, just mount empty directory
            Volume index = new Volume();
            index.setName( cfgStr( "operator.deployment.xp.volume.index.name" ) );
            index.setEmptyDir( new EmptyDirVolumeSource( null, null ) );
            res.add( index );
        }

        if ( snapshotsPvcName().isPresent() )
        {
            Volume snapshots = new Volume();
            snapshots.setName( cfgStr( "operator.deployment.xp.volume.snapshots.name" ) );
            snapshots.setPersistentVolumeClaim( new PersistentVolumeClaimVolumeSource( snapshotsPvcName().get(), false ) );
            res.add( snapshots );
        }

        return res;
    }

    protected List<PersistentVolumeClaim> createVolumeClaimTemplates()
    {
        List<PersistentVolumeClaim> res = new LinkedList<>();
        if ( indexDiskSize().isPresent() )
        {
            res.add( standard( cfgStr( "operator.deployment.xp.volume.index.name" ), podLabels(),
                               cfgStr( "operator.deployment.xp.volume.defaultStorageClass" ), "ReadWriteOnce", indexDiskSize().get() ) );
        }
        return res;
    }
}
