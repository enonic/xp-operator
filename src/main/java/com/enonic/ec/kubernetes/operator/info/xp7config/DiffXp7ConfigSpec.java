package com.enonic.ec.kubernetes.operator.info.xp7config;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7config.spec.Xp7ConfigSpec;

@Value.Immutable
public abstract class DiffXp7ConfigSpec
    extends Diff<Xp7ConfigSpec>
{
}
