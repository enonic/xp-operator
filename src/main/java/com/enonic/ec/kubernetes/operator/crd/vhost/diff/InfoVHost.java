package com.enonic.ec.kubernetes.operator.crd.vhost.diff;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoVHost
    extends ResourceInfoNamespaced<XpVHostResource, DiffResource>
{
    @Override
    protected DiffResource createDiff( final Optional<XpVHostResource> oldResource, final Optional<XpVHostResource> newResource )
    {
        return ImmutableDiffResource.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
