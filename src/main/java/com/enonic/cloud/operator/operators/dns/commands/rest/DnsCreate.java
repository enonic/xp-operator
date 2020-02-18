package com.enonic.cloud.operator.operators.dns.commands.rest;

import org.immutables.value.Value;

import com.enonic.cloud.operator.operators.dns.cloudflare.DnsRecordService;
import com.enonic.cloud.operator.operators.dns.cloudflare.model.DnsRecord;

@Value.Immutable
public abstract class DnsCreate
    extends DnsCommand
{
    protected abstract DnsRecordService dnsRecordsService();

    protected abstract String zoneId();

    protected abstract DnsRecord dnsRecord();

    @Override
    public void execute()
    {
        dnsRecordsService().create( zoneId(), dnsRecord() );
    }

    @Override
    protected String action()
    {
        return "CREATE";
    }

    @SuppressWarnings("EmptyMethod") // Immutables will complain if we remove this method
    @Override
    public String toString()
    {
        return super.toString();
    }
}
