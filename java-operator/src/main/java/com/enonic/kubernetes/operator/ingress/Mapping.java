package com.enonic.kubernetes.operator.ingress;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.enonic.kubernetes.common.annotations.Params;

@Value.Immutable
@Params
public interface Mapping
{
    String name();

    @Nullable
    String host();

    String source();

    String target();

    @Nullable
    String idProviders();
}
