package com.enonic.ec.kubernetes.operator.dns;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;

@Value.Immutable
public abstract class DiffDnsIngress
    extends Diff<DnsIngress>
{

    @Value.Derived
    public boolean ipChanged()
    {
        return !equals( DnsIngress::ips );
    }

}
