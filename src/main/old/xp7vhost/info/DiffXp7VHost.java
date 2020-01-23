package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;

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
