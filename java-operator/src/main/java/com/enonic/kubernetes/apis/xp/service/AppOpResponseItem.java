package com.enonic.kubernetes.apis.xp.service;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppOpResponseItem.Builder.class)
@Value.Immutable
public abstract class AppOpResponseItem
{
    public abstract String id();

    public abstract boolean success();
}
