package com.enonic.kubernetes.apis.xp;

import org.immutables.value.Value;

import com.enonic.kubernetes.common.annotations.Params;

@Value.Immutable
@Params
public interface XpClientCacheKey
{
    String namespace();

    String nodeGroup();
}
