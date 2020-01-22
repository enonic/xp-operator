package com.enonic.ec.kubernetes.operator.api.conversion.model;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.HasMetadata;

@JsonDeserialize(builder = ImmutableConversionReviewResponse.Builder.class)
@Value.Immutable
public abstract class ConversionReviewResponse
{
    public abstract String uid();

    public abstract ConversionReviewResponseResult result();

    @Nullable
    public abstract List<HasMetadata> convertedObjects();
}
