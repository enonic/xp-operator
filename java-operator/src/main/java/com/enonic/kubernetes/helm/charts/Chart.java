package com.enonic.kubernetes.helm.charts;

import org.immutables.value.Value;

import com.enonic.kubernetes.common.annotations.Params;

@Value.Immutable
@Params
public abstract class Chart
{
    public abstract String uri();
}
