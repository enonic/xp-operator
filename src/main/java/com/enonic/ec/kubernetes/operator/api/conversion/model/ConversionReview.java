package com.enonic.ec.kubernetes.operator.api.conversion.model;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableConversionReview.Builder.class)
@Value.Immutable
public abstract class ConversionReview
{
    @Value.Default
    public String apiVersion()
    {
        return "apiextensions.k8s.io/v1";
    }

    @Value.Default
    public String kind()
    {
        return "ConversionReview";
    }

    @Nullable
    public abstract ConversionReviewRequest request();

    @Nullable
    public abstract ConversionReviewResponse response();
}
