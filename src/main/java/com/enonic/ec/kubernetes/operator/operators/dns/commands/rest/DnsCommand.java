package com.enonic.ec.kubernetes.operator.operators.dns.commands.rest;

import com.enonic.ec.kubernetes.operator.common.commands.Command;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.DnsRecordService;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.model.DnsRecord;

public abstract class DnsCommand
    implements Command
{
    protected abstract DnsRecordService dnsRecordsService();

    protected abstract DnsRecord dnsRecord();

    String action()
    {
        throw new RuntimeException( "Override this method" );
    }

    @Override
    public String toString()
    {
        return action() + " DNS record " + dnsRecord();
    }
}
