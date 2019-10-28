package com.enonic.ec.kubernetes.common;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Tuple<A, B>
{
    @Value.Parameter
    public abstract A a();

    @Value.Parameter
    public abstract B b();
}
