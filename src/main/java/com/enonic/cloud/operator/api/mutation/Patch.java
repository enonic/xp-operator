package com.enonic.cloud.operator.api.mutation;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public abstract class Patch
{
    public abstract String op();

    public abstract String path();

    public abstract String value();
}
