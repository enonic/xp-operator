package com.enonic.ec.kubernetes.apis.cloudflare.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableApiResponseError.Builder.class)
@Value.Immutable
public abstract class ApiResponseError
{
    public abstract Integer code();

    public abstract String message();
}
