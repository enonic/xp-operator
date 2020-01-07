package com.enonic.ec.kubernetes.operator.helm;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Chart
{
    abstract String uri();
}
