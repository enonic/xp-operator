package com.enonic.cloud.apis.xp;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface XpClientCacheKey
{
    String namespace();

    String nodeGroup();
}
