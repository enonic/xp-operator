package com.enonic.ec.kubernetes.crd.vhost.spec;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableSpecMapping.Builder.class)
@Value.Immutable
public abstract class SpecMapping
{
    public abstract String name();

    public abstract String node();

    public abstract String source();

    public abstract String target();

    @Nullable
    public abstract String idProvider(); // TODO: Fix Nullable
}
