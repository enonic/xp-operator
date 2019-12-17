package com.enonic.ec.kubernetes.operator.crd.app.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpAppResource>
{
    @Value.Derived
    public ImmutableDiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( oldValue().map( XpAppResource::getSpec ) ).
            newValue( newValue().map( XpAppResource::getSpec ) ).
            build();
    }
}
