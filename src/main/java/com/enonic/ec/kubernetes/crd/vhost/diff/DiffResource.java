package com.enonic.ec.kubernetes.crd.vhost.diff;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpVHostResource>
{
    @Value.Derived
    public ImmutableDiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( Optional.ofNullable( oldValue().map( XpVHostResource::getSpec ).orElse( null ) ) ).
            newValue( Optional.ofNullable( newValue().map( XpVHostResource::getSpec ).orElse( null ) ) ).
            build();
    }

}
