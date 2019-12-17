package com.enonic.ec.kubernetes.operator.info.xp7app;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7app.spec.Xp7AppSpec;
import com.enonic.ec.kubernetes.operator.info.Diff;

@Value.Immutable
public abstract class DiffXp7AppSpec
    extends Diff<Xp7AppSpec>
{
}
