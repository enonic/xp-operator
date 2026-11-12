package com.enonic.kubernetes.operator.api.mutation;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.kubernetes.common.annotations.Params;

@JsonDeserialize(builder = PatchImpl.Builder.class)
@Value.Immutable
@Params
public abstract class Patch
{
    public abstract String op();

    public abstract String path();

    @Nullable
    public abstract Object value();
}
