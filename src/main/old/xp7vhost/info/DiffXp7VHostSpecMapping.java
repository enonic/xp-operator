package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecMapping;

@Value.Immutable
public abstract class DiffXp7VHostSpecMapping
    extends Diff<Xp7VHostSpecMapping>
{
}
