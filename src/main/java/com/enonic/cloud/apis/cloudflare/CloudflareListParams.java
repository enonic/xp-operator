package com.enonic.cloud.apis.cloudflare;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface CloudflareListParams
{
    String zoneId();

    String name();

    @Nullable
    String type();
}
