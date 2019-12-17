package com.enonic.ec.kubernetes.operator.info.xp7vhost;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;

@Value.Immutable
public abstract class DiffXp7VHost
    extends Diff<Xp7VHostResource>
{
    @Value.Derived
    public DiffXp7VHostSpec diffSpec()
    {
        return ImmutableDiffXp7VHostSpec.builder().
            oldValue( oldValue().map( Xp7VHostResource::getSpec ) ).
            newValue( newValue().map( Xp7VHostResource::getSpec ) ).
            build();
    }

}
