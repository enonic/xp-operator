package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableDohQuestion.Builder.class)
@Value.Immutable
public abstract class DohQuestion
{
    public abstract Integer type();

    public abstract String name();
}
