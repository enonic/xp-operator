package com.enonic.ec.kubernetes.operator.crd.config.diff;

import java.util.Collections;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoConfig
    extends ResourceInfoNamespaced<XpConfigResource, DiffResource>
{
    @Override
    protected DiffResource createDiff( final Optional<XpConfigResource> oldResource, final Optional<XpConfigResource> newResource )
    {
        return ImmutableDiffResource.builder().
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
