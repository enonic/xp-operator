package com.enonic.ec.kubernetes.api;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.crd.deployment.spec.Spec;

@JsonDeserialize(builder = ImmutableXpDeploymentJson.Builder.class)
@Value.Immutable
public interface XpDeploymentJson
{
    String apiVersion();

    Optional<String> uid();

    Spec spec();
}