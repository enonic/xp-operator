package com.enonic.ec.kubernetes.dns.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;

@Value.Immutable
public abstract class DiffDnsIngress
    extends Diff<DnsIngress>
{
    @Value.Derived
    public List<DiffDnsIngressDomains> diffDomains()
    {
        List<Domain> oldDomains = oldValue().map( DnsIngress::domains ).orElse( Collections.emptyList() );
        List<Domain> newDomains = newValue().map( DnsIngress::domains ).orElse( Collections.emptyList() );

        List<DiffDnsIngressDomains> res = mergeLists( oldDomains, newDomains, ( o, n ) -> ImmutableDiffDnsIngressDomains.builder().
            oldValue( o ).
            newValue( n ).
            build() );
        res.sort( Comparator.comparing( DiffDnsIngressDomains::domain ) );
        return res;
    }

    @Value.Derived
    public List<DiffDnsIngressIps> diffIps()
    {
        @SuppressWarnings("unchecked") List<String> oldIps = oldValue().map( DnsIngress::ips ).orElse( Collections.EMPTY_LIST );
        @SuppressWarnings("unchecked") List<String> newIps = newValue().map( DnsIngress::ips ).orElse( Collections.EMPTY_LIST );

        List<DiffDnsIngressIps> res = mergeLists( oldIps, newIps, ( o, n ) -> ImmutableDiffDnsIngressIps.builder().
            oldValue( o ).
            newValue( n ).
            build() );
        res.sort( Comparator.comparing( DiffDnsIngressIps::ip ) );
        return res;
    }

    @Value.Derived
    public boolean ipsChanged()
    {
        return !equals( DnsIngress::ips );
    }

    @Value.Derived
    public boolean domainsChanged()
    {
        return !equals( DnsIngress::domains );
    }

}
