package com.enonic.cloud.apis.doh.service;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableDohQuestion.Builder.class)
@Value.Immutable
public abstract class DohQuestion
{
    public abstract Integer type();

    public abstract String name();
}
