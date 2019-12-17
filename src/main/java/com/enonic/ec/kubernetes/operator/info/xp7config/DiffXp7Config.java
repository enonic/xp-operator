package com.enonic.ec.kubernetes.operator.info.xp7config;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResource;

@Value.Immutable
public abstract class DiffXp7Config
    extends Diff<Xp7ConfigResource>
{
    @Value.Derived
    public DiffXp7ConfigSpec diffSpec()
    {
        return ImmutableDiffXp7ConfigSpec.builder().
            oldValue( oldValue().map( Xp7ConfigResource::getSpec ) ).
            newValue( newValue().map( Xp7ConfigResource::getSpec ) ).
            build();
    }
}
