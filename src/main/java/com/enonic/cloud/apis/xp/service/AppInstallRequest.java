package com.enonic.cloud.apis.xp.service;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppInstallRequest.Builder.class)
@Value.Immutable
public abstract class AppInstallRequest
{
    @JsonProperty("URL")
    public abstract String url();
}