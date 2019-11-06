package com.enonic.ec.kubernetes.operator.commands.builders.spec.volumes;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class VolumeBuilder
    extends Configuration
{
    protected abstract String deploymentName();

    public VolumeTripletList getVolumeTriplets( final String configMapName, final Optional<Quantity> persistIndexSize )
    {
        VolumeTripletList list = new VolumeTripletList();
        list.add( xpConfig( configMapName ) );
        list.add( xpDeploy() );
        list.add( xpIndex( persistIndexSize ) );
        list.add( xpShared() );
        return list;
    }

    static PersistentVolumeClaim standard( String name, Map<String, String> labels, String storageClassName, String accessMode,
                                           Quantity size )
    {
        PersistentVolumeClaim claim = new PersistentVolumeClaim();
        ObjectMeta meta = new ObjectMeta();
        claim.setMetadata( meta );
        meta.setName( name );
        meta.setLabels( labels );

        PersistentVolumeClaimSpec spec = new PersistentVolumeClaimSpec();
        claim.setSpec( spec );
        spec.setAccessModes( Collections.singletonList( accessMode ) );
        spec.setStorageClassName( storageClassName );
        spec.setResources( new ResourceRequirements( null, Map.of( "storage", size ) ) );

        return claim;
    }


    private VolumeTriplet xpConfig( final String configMapName )
    {
        String name = cfgStr( "operator.deployment.xp.volume.config.name" );
        String path = cfgStr( "operator.deployment.xp.volume.config.path" );

        VolumeMount volumeMount = new VolumeMount();
        volumeMount.setName( name );
        volumeMount.setMountPath( path );

        Volume volume = new Volume();
        volume.setName( name );
        volume.setConfigMap( new ConfigMapVolumeSource( null, null, configMapName, null ) );

        return ImmutableVolumeTriplet.builder().
            addVolumeMount( volumeMount ).
            volume( volume ).
            build();
    }

    private VolumeTriplet xpDeploy()
    {
        String name = cfgStr( "operator.deployment.xp.volume.deploy.name" );
        String path = cfgStr( "operator.deployment.xp.volume.deploy.path" );

        VolumeMount volumeMount = new VolumeMount();
        volumeMount.setName( name );
        volumeMount.setMountPath( path );

        Volume volume = new Volume();
        volume.setName( name );
        volume.setEmptyDir( new EmptyDirVolumeSource( null, null ) );

        return ImmutableVolumeTriplet.builder().
            addVolumeMount( volumeMount ).
            volume( volume ).
            build();
    }

    private VolumeTriplet xpIndex( final Optional<Quantity> persistIndexSize )
    {
        String name = cfgStr( "operator.deployment.xp.volume.index.name" );
        String path = cfgStr( "operator.deployment.xp.volume.index.path" );

        VolumeMount volumeMount = new VolumeMount();
        volumeMount.setName( name );
        volumeMount.setMountPath( path );

        if ( persistIndexSize.isEmpty() )
        {
            // Index is not persisted, just mount empty directory
            Volume volume = new Volume();
            volume.setName( name );
            volume.setEmptyDir( new EmptyDirVolumeSource( null, null ) );

            return ImmutableVolumeTriplet.builder().
                addVolumeMount( volumeMount ).
                volume( volume ).
                build();
        }
        else
        {
            PersistentVolumeClaim index = standard( cfgStr( "operator.deployment.xp.volume.index.name" ), Map.of(), // TODO: Labels
                                                    cfgStr( "operator.deployment.xp.volume.defaultStorageClass" ), "ReadWriteOnce",
                                                    persistIndexSize.get() );
            return ImmutableVolumeTriplet.builder().
                addVolumeMount( volumeMount ).
                volumeClaim( index ).
                build();
        }
    }

    protected abstract Volume sharedVolume( String name );

    private VolumeTriplet xpShared()
    {
        String name = cfgStr( "operator.deployment.xp.volume.shared.name" );

        VolumeMount blobVolumeMount = new VolumeMount();
        blobVolumeMount.setName( name );
        blobVolumeMount.setMountPath( cfgStr( "operator.deployment.xp.volume.shared.blob.path" ) );
        blobVolumeMount.setSubPath( cfgStrFmt( "operator.deployment.xp.volume.shared.blob.pvcSubPath", deploymentName() ) );

        VolumeMount snapshotVolumeMount = new VolumeMount();
        snapshotVolumeMount.setName( name );
        snapshotVolumeMount.setMountPath( cfgStr( "operator.deployment.xp.volume.shared.snapshots.path" ) );
        snapshotVolumeMount.setSubPath( cfgStrFmt( "operator.deployment.xp.volume.shared.snapshots.pvcSubPath", deploymentName() ) );

        return ImmutableVolumeTriplet.builder().
            addVolumeMount( blobVolumeMount ).
            addVolumeMount( snapshotVolumeMount ).
            volume( sharedVolume( name ) ).
            build();
    }

}
