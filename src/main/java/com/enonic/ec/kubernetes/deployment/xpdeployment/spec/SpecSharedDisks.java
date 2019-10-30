package com.enonic.ec.kubernetes.deployment.xpdeployment.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

@JsonDeserialize(builder = ImmutableSpecSharedDisks.Builder.class)
@Value.Immutable
public abstract class SpecSharedDisks
{
    public abstract Quantity blob();

    public abstract Quantity snapshots();
}
