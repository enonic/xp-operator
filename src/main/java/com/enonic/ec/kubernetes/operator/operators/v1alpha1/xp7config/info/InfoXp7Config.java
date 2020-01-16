package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info;

import java.util.Collections;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;

@Value.Immutable
public abstract class InfoXp7Config
    extends ResourceInfoNamespaced<Xp7ConfigResource, DiffXp7Config>
{
    @Override
    protected DiffXp7Config createDiff( final Optional<Xp7ConfigResource> oldResource, final Optional<Xp7ConfigResource> newResource )
    {
        return ImmutableDiffXp7Config.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( config -> checkNode( true, Collections.singletonList( config.getSpec().node() ) ) );
    }
}
