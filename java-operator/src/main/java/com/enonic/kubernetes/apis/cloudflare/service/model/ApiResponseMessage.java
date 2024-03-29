package com.enonic.kubernetes.apis.cloudflare.service.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableApiResponseMessage.Builder.class)
@Value.Immutable
public abstract class ApiResponseMessage
{
    public abstract Integer code();

    public abstract String message();
}
