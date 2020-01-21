package com.enonic.ec.kubernetes.operator.api.conversion.model;

import java.util.List;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

@Value.Immutable
public abstract class ConversionReviewRequest
{
    public abstract String uid();

    public abstract String desiredAPIVersion();

    public abstract List<HasMetadata> objects();
}
