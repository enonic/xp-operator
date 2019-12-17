package com.enonic.ec.kubernetes.operator.info.xp7app;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

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
