package com.enonic.ec.kubernetes.deployment.xpdeployment;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecSharedDisks.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecSharedDisks
{
    public abstract Quantity blob();
}
