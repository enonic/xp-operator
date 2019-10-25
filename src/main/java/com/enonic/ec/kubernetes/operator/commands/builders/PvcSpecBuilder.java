package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class PvcSpecBuilder
    extends Configuration
    implements Command<PersistentVolumeClaimSpec>
{
    protected abstract List<String> accessMode();

    protected abstract Quantity size();

    @Override
    public PersistentVolumeClaimSpec execute()
    {
        PersistentVolumeClaimSpec spec = new PersistentVolumeClaimSpec();
        spec.setStorageClassName( cfgStr( "operator.deployment.xp.volume.defaultStorageClass" ) );
        spec.setResources( new ResourceRequirements( null, Map.of( "storage", size() ) ) );
        spec.setAccessModes( accessMode() );
        return spec;
    }
}
