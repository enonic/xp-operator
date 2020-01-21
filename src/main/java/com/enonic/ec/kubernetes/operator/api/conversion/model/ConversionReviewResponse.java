package com.enonic.ec.kubernetes.operator.api.conversion.model;

import java.util.List;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

@Value.Immutable
public abstract class ConversionReviewResponse
{
    public abstract String uid();

    public abstract ConversionReviewResponseResult result();

    public abstract List<HasMetadata> convertedObjects();
}
