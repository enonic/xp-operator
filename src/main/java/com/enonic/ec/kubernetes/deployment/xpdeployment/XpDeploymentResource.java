package com.enonic.ec.kubernetes.deployment.xpdeployment;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.client.CustomResource;

@JsonDeserialize(builder = ImmutableXpDeploymentResource.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResource
    extends CustomResource
{

    public abstract XpDeploymentResourceSpec spec();

    @Override
    public abstract String toString();
}
