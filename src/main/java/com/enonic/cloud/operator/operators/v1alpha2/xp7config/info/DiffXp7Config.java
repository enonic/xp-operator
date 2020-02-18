package com.enonic.cloud.operator.operators.v1alpha2.xp7config.info;

import org.immutables.value.Value;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;

@Value.Immutable
public abstract class DiffXp7Config
    extends Diff<V1alpha2Xp7Config>
{
    @Value.Derived
    public DiffXp7ConfigSpec diffSpec()
    {
        return ImmutableDiffXp7ConfigSpec.builder().
            oldValue( oldValue().map( V1alpha2Xp7Config::getSpec ) ).
            newValue( newValue().map( V1alpha2Xp7Config::getSpec ) ).
            build();
    }
}
