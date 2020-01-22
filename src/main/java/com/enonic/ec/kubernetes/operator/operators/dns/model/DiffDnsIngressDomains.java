package com.enonic.ec.kubernetes.operator.operators.dns.model;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.Diff;

@Value.Immutable
public abstract class DiffDnsIngressDomains
    extends Diff<Domain>
{
    @Value.Derived
    public String domain()
    {
        return newValue().map( Domain::domain ).orElse( oldValue().map( Domain::domain ).orElse( null ) );
    }

    @Value.Derived
    public String zoneId()
    {
        return newValue().map( Domain::zoneId ).orElse( oldValue().map( Domain::zoneId ).orElse( null ) );
    }
}
