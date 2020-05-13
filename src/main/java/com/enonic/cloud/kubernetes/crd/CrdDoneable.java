package com.enonic.cloud.kubernetes.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;


public class CrdDoneable<T extends HasMetadata>
    implements Doneable<T>
{
    protected final T resource;

    private final Function<T, T> function;

    @SuppressWarnings("WeakerAccess")
    public CrdDoneable( T resource, Function<T, T> function )
    {
        this.resource = resource;
        this.function = function;
    }

    @Override
    public T done()
    {
        return function.apply( resource );
    }
}
