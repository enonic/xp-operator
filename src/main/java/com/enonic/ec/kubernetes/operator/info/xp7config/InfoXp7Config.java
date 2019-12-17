package com.enonic.ec.kubernetes.operator.info.xp7config;

import java.util.Collections;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

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
