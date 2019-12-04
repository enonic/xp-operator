package com.enonic.ec.kubernetes.operator.crd.vhost.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpVHostResource>
{
    @Value.Derived
    public ImmutableDiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( oldValue().map( XpVHostResource::getSpec ) ).
            newValue( newValue().map( XpVHostResource::getSpec ) ).
            build();
    }

}
