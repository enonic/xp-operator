package com.enonic.ec.kubernetes.crd.vhost.spec;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableSpecMapping.Builder.class)
@Value.Immutable
public abstract class SpecMapping
{
    public abstract String name();

    public abstract String nodeAlias();

    public abstract String source();

    public abstract String target();

    public abstract Optional<String> idProvider();
}
