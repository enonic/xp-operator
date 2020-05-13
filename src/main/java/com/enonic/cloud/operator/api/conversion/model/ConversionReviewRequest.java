package com.enonic.cloud.operator.api.conversion.model;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.HasMetadata;

@JsonDeserialize(builder = ImmutableConversionReviewRequest.Builder.class)
@Value.Immutable
public abstract class ConversionReviewRequest
{
    public abstract String uid();

    public abstract String desiredAPIVersion();

    public abstract List<HasMetadata> objects();
}
