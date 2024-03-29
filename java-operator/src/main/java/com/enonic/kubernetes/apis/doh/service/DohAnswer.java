package com.enonic.kubernetes.apis.doh.service;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableDohAnswer.Builder.class)
@Value.Immutable
public abstract class DohAnswer
{
    public abstract Integer type();

    public abstract String name();

    @JsonProperty("TTL")
    public abstract String ttl();

    public abstract String data();
}
