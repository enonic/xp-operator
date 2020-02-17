package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoXp7VHost
    extends ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost>
{
    @Override
    protected DiffXp7VHost createDiff( final Optional<V1alpha2Xp7VHost> oldResource, final Optional<V1alpha2Xp7VHost> newResource )
    {
        return ImmutableDiffXp7VHost.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
