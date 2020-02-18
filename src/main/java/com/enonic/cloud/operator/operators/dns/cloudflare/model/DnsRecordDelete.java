package com.enonic.cloud.operator.operators.dns.cloudflare.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableDnsRecordDelete.Builder.class)
@Value.Immutable
public abstract class DnsRecordDelete
{
    public abstract String id();
}