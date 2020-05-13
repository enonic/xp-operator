package com.enonic.cloud.apis.cloudflare.service.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableApiResponseInfo.Builder.class)
@Value.Immutable
public abstract class ApiResponseInfo
{
    public abstract Integer page();

    public abstract Integer per_page();

    public abstract Integer count();

    public abstract Integer total_count();

    public abstract Integer total_pages();
}
