package com.enonic.ec.kubernetes.operator.crd.config.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpConfigResource>
{
}
