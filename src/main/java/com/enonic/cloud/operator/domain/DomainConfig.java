package com.enonic.cloud.operator.domain;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface DomainConfig
{
    String domain();

    String zoneId();
}
