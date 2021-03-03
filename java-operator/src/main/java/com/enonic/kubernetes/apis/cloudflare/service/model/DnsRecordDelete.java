package com.enonic.kubernetes.apis.cloudflare.service.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableDnsRecordDelete.Builder.class)
@Value.Immutable
public abstract class DnsRecordDelete
{
    public abstract String id();
}
