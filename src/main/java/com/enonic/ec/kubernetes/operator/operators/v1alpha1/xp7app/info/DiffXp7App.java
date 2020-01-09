package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.info.Diff;

@Value.Immutable
public abstract class DiffXp7App
    extends Diff<Xp7AppResource>
{
    @Value.Derived
    public DiffXp7AppSpec diffSpec()
    {
        return ImmutableDiffXp7AppSpec.builder().
            oldValue( oldValue().map( Xp7AppResource::getSpec ) ).
            newValue( newValue().map( Xp7AppResource::getSpec ) ).
            build();
    }
}
