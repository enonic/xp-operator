package com.enonic.kubernetes.apis.xp.service;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppOpResponse.Builder.class)
@Value.Immutable
public abstract class AppOpResponse
{
    public abstract List<AppOpResponseItem> results();
}
