package com.enonic.cloud.helm.charts;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public abstract class Chart
{
    public abstract String uri();
}
