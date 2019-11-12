package com.enonic.ec.kubernetes.api;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;


@JsonDeserialize(builder = ImmutableXpVHostJson.Builder.class)
@Value.Immutable
public interface XpVHostJson
{
    String apiVersion();

    Optional<String> uid();

    Spec spec();
}