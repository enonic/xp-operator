package com.enonic.ec.kubernetes.operator.crd.vhost.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.SpecMapping;

@Value.Immutable
public abstract class DiffSpecMapping
    extends Diff<SpecMapping>
{
}
