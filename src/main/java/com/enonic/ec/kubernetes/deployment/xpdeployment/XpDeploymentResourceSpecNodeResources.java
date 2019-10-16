package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecNodeResources.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecNodeResources
{
    public abstract Quantity cpu();

    public abstract Quantity memory();

    public abstract Map<String, Quantity> disks();

}
