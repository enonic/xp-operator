package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.spec.Xp7ConfigSpec;

@Value.Immutable
public abstract class DiffXp7ConfigSpec
    extends Diff<Xp7ConfigSpec>
{
}
