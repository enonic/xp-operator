package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.Xp7AppResource;

@Value.Immutable
public abstract class InfoXp7App
    extends ResourceInfoNamespaced<Xp7AppResource, DiffXp7App>
{
    @Override
    protected DiffXp7App createDiff( final Optional<Xp7AppResource> oldResource, final Optional<Xp7AppResource> newResource )
    {
        return ImmutableDiffXp7App.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
