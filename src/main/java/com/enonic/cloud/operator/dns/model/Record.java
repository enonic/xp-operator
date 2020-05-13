package com.enonic.cloud.operator.dns.model;

import java.util.Set;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface Record
{
    Integer ttl();

    Set<String> ips();

    Boolean cdn();

    String heritage();
}
