package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.cloud.operator.common.info.Diff;

@Value.Immutable
public abstract class DiffConfigMap
    extends Diff<ConfigMap>
{
}
