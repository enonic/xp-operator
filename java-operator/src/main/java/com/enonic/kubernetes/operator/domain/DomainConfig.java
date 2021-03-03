package com.enonic.kubernetes.operator.domain;

import org.immutables.value.Value;

import com.enonic.kubernetes.common.annotations.Params;

@Value.Immutable
@Params
public interface DomainConfig
{
    String domain();

    String zoneId();
}
