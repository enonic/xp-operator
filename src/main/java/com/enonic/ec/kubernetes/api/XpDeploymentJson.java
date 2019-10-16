package com.enonic.ec.kubernetes.api;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpec;

@JsonDeserialize(builder = ImmutableXpDeploymentJson.Builder.class)
@Value.Immutable
public interface XpDeploymentJson
{

    @Nullable
    String uid();

    String apiVersion();

    XpDeploymentResourceSpec spec();

}