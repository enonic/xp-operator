package com.enonic.kubernetes.apis.xp;

import com.enonic.kubernetes.common.annotations.Params;
import org.immutables.value.Value;

@Value.Immutable
@Params
public interface XpClientCacheKey
{
    String namespace();

    String nodeGroup();
}
