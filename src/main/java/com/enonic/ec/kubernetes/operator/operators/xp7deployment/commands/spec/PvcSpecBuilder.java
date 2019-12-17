package com.enonic.ec.kubernetes.operator.operators.xp7deployment.commands.spec;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;

import com.enonic.ec.kubernetes.operator.common.Configuration;

@Value.Immutable
public abstract class PvcSpecBuilder
    extends Configuration
{
    protected abstract List<String> accessMode();

    protected abstract Quantity size();

    @Value.Derived
    public PersistentVolumeClaimSpec spec()
    {
        PersistentVolumeClaimSpec spec = new PersistentVolumeClaimSpec();
        spec.setStorageClassName( cfgStr( "operator.deployment.xp.volume.defaultStorageClass" ) );
        spec.setResources( new ResourceRequirements( null, Map.of( "storage", size() ) ) );
        spec.setAccessModes( accessMode() );
        return spec;
    }
}
