package com.enonic.ec.kubernetes.operator.crd.config.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.config.spec.Spec;

@Value.Immutable
public abstract class DiffSpec
    extends Diff<Spec>
{
}
