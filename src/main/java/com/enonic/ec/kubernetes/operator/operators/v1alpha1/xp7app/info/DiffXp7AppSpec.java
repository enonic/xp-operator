package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7AppSpec;

@Value.Immutable
public abstract class DiffXp7AppSpec
    extends Diff<V1alpha1Xp7AppSpec>
{
}
