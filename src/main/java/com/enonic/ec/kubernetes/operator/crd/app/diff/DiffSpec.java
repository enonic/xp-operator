package com.enonic.ec.kubernetes.operator.crd.app.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.app.spec.Spec;

@Value.Immutable
public abstract class DiffSpec
    extends Diff<Spec>
{
}
