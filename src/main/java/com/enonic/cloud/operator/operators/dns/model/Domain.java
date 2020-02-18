package com.enonic.cloud.operator.operators.dns.model;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Domain
{
    public abstract String domain();

    public abstract String zoneId();
}
