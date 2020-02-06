package com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.model;

import java.util.Map;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableDnsRecord.Builder.class)
@Value.Immutable
public abstract class DnsRecord
{
    @Nullable
    public abstract String id();

    public abstract String type();

    public abstract String name();

    public abstract String content();

    @SuppressWarnings("SpellCheckingInspection") // That's what its called in the cloudflare API
    @Nullable
    public abstract Boolean proxiable();

    @Nullable
    public abstract Boolean proxied();

    @Nullable
    public abstract Integer ttl();

    @Nullable
    public abstract Integer priority();

    @Nullable
    public abstract Boolean locked();

    @Nullable
    public abstract String zone_id();

    @Nullable
    public abstract String zone_name();

    @Nullable
    public abstract String created_on();

    @Nullable
    public abstract String modified_on();

    @Nullable
    public abstract Map<String, Object> data();

    @Nullable
    public abstract Map<String, Object> meta();

    @Override
    public String toString()
    {
        return String.format( "%s {%s, %s}", name(), type(), content() );
    }
}
