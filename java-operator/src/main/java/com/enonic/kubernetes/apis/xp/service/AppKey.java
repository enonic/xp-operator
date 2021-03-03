package com.enonic.kubernetes.apis.xp.service;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppKey.Builder.class)
@Value.Immutable
public abstract class AppKey
{
    @JsonProperty("key")
    public abstract String key();
}
