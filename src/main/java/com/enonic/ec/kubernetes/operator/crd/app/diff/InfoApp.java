package com.enonic.ec.kubernetes.operator.crd.app.diff;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoApp
    extends ResourceInfoNamespaced<XpAppResource, DiffResource>
{
    @Override
    protected DiffResource createDiff( final Optional<XpAppResource> oldResource, final Optional<XpAppResource> newResource )
    {
        return ImmutableDiffResource.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
