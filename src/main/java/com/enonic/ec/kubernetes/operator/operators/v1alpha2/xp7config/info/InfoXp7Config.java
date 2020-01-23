package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info;

import java.util.Collections;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoXp7Config
    extends ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config>
{
    @Override
    protected DiffXp7Config createDiff( final Optional<V1alpha2Xp7Config> oldResource, final Optional<V1alpha2Xp7Config> newResource )
    {
        return ImmutableDiffXp7Config.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( config -> checkNode( true, Collections.singletonList( config.getSpec().nodeGroup() ) ) );
    }
}
