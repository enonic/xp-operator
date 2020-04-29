package com.enonic.cloud.operator.operators.v1alpha2.xp7config.info;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoConfigMap
    extends ResourceInfoNamespaced<ConfigMap, DiffConfigMap>
{
    @Override
    protected DiffConfigMap createDiff( final Optional<ConfigMap> oldResource, final Optional<ConfigMap> newResource )
    {
        return ImmutableDiffConfigMap.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
