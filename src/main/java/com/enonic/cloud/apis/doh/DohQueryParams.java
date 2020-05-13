package com.enonic.cloud.apis.doh;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface DohQueryParams
{
    @Nullable
    String type();

    String name();
}
