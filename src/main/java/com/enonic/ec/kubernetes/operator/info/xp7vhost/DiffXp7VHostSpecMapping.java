package com.enonic.ec.kubernetes.operator.info.xp7vhost;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.Xp7VHostSpecMapping;

@Value.Immutable
public abstract class DiffXp7VHostSpecMapping
    extends Diff<Xp7VHostSpecMapping>
{
}
