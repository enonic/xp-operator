package com.enonic.ec.kubernetes.operator.api.conversion.model;

import java.util.Map;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableConversionReviewResponseResult.Builder.class)
@Value.Immutable
public abstract class ConversionReviewResponseResult
{
    @Nullable
    public abstract String status();

    @Nullable
    public abstract String message();

    @Nullable
    public abstract Map<String, String> metadata();
}
