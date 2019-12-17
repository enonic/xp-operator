package com.enonic.ec.kubernetes.dns.model;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;

@Value.Immutable
public abstract class DiffDnsIngressIps
    extends Diff<String>
{
    @Value.Derived
    public String ip()
    {
        return newValue().orElse( oldValue().orElse( null ) );
    }
}
