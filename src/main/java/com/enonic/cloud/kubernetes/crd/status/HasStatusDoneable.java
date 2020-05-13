package com.enonic.cloud.kubernetes.crd.status;

import io.fabric8.kubernetes.api.builder.Function;

import com.enonic.cloud.kubernetes.crd.CrdDoneable;

public abstract class HasStatusDoneable<F, S extends CrdStatus<F>, R extends HasStatus<F, S>>
    extends CrdDoneable<R>
{
    @SuppressWarnings("WeakerAccess")
    public HasStatusDoneable( final R resource, final Function<R, R> function )
    {
        super( resource, function );
    }

    public HasStatusDoneable<F, S, R> withStatus( S status )
    {
        resource.setStatus( status );
        return this;
    }
}
