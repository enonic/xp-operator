package com.enonic.ec.kubernetes.dns.commands.rest;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.dns.cloudflare.DnsRecordService;
import com.enonic.ec.kubernetes.dns.cloudflare.model.DnsRecord;

public abstract class DnsCommand
    implements Command<Void>
{
    protected abstract DnsRecordService dnsRecordsService();

    protected abstract DnsRecord dnsRecord();

    protected String action()
    {
        throw new RuntimeException( "Override this method" );
    }

    @Override
    public String toString()
    {
        return action() + " DNS record " + dnsRecord();
    }
}
