package com.enonic.ec.kubernetes.operator.dns.cloudflare;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.dns.cloudflare.DnsRecords;
import com.enonic.ec.kubernetes.dns.cloudflare.model.DnsRecord;

@Value.Immutable
public abstract class DnsCreate
    extends DnsCommand
{
    protected abstract DnsRecords dnsRecordsService();

    protected abstract String zoneId();

    protected abstract DnsRecord dnsRecord();

    @Override
    public Void execute()
    {
        dnsRecordsService().create( zoneId(), dnsRecord() );
        return null;
    }

    @Override
    protected String action()
    {
        return "CREATE";
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
