package com.enonic.ec.kubernetes.operator.operators.dns.commands.rest;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.DnsRecordService;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.model.DnsRecord;

@Value.Immutable
public abstract class DnsDelete
    extends DnsCommand
{
    protected abstract DnsRecordService dnsRecordsService();

    protected abstract String zoneId();

    protected abstract DnsRecord dnsRecord();

    @Override
    public void execute()
    {
        dnsRecordsService().delete( zoneId(), dnsRecord().id() );
    }

    @Override
    protected String action()
    {
        return "DELETE";
    }

    @SuppressWarnings("EmptyMethod") // Immutables will complain if we remove this method
    @Override
    public String toString()
    {
        return super.toString();
    }
}
