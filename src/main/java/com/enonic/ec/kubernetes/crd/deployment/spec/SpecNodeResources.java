package com.enonic.ec.kubernetes.crd.deployment.spec;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

@JsonDeserialize(builder = ImmutableSpecNodeResources.Builder.class)
@Value.Immutable
public abstract class SpecNodeResources
{
    public abstract Quantity cpu();

    public abstract Quantity memory();

    public abstract Map<String, Quantity> disks();
}
