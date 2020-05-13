package com.enonic.cloud.operator.dns.model;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface Domain
{
    String domain();

    String zoneId();
}
