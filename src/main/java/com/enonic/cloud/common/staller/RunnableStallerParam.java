package com.enonic.cloud.common.staller;

import java.time.Instant;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface RunnableStallerParam
{
    Instant timestamp();

    Runnable runnable();
}
