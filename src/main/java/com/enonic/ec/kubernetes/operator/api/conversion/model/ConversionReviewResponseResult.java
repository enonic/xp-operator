package com.enonic.ec.kubernetes.operator.api.conversion.model;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

@Value.Immutable
public abstract class ConversionReviewResponseResult
{
    public abstract String status();

    @Nullable
    public abstract String message();
}
