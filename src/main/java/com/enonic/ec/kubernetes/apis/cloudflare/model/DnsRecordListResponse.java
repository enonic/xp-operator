package com.enonic.ec.kubernetes.apis.cloudflare.model;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableDnsRecordListResponse.Builder.class)
@Value.Immutable
public abstract class DnsRecordListResponse
    extends ApiResponse<List<DnsRecord>>
{
}
