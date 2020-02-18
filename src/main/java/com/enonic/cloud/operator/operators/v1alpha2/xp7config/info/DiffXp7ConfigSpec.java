package com.enonic.cloud.operator.operators.v1alpha2.xp7config.info;

import org.immutables.value.Value;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7ConfigSpec;

@Value.Immutable
public abstract class DiffXp7ConfigSpec
    extends Diff<V1alpha2Xp7ConfigSpec>
{
}