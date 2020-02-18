package com.enonic.cloud.operator.operators.v1alpha1.xp7app.info;

import org.immutables.value.Value;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;

@Value.Immutable
public abstract class DiffXp7App
    extends Diff<V1alpha1Xp7App>
{
    @Value.Derived
    public DiffXp7AppSpec diffSpec()
    {
        return ImmutableDiffXp7AppSpec.builder().
            oldValue( oldValue().map( V1alpha1Xp7App::getSpec ) ).
            newValue( newValue().map( V1alpha1Xp7App::getSpec ) ).
            build();
    }
}
