package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;

@Value.Immutable
public abstract class DiffXp7VHostSpecMapping
    extends Diff<V1alpha2Xp7VHostSpecMapping>
{
}
