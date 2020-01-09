package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.spec.Xp7AppSpec;
import com.enonic.ec.kubernetes.operator.info.Diff;

@Value.Immutable
public abstract class DiffXp7AppSpec
    extends Diff<Xp7AppSpec>
{
}
