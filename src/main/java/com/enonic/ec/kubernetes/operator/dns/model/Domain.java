package com.enonic.ec.kubernetes.operator.dns.model;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Domain
{
    public abstract String domain();

    public abstract String zoneId();
}
