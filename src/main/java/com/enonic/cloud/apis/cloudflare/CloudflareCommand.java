package com.enonic.cloud.apis.cloudflare;

import javax.ws.rs.WebApplicationException;

import org.immutables.value.Value;

import com.enonic.cloud.apis.cloudflare.service.model.DnsRecord;
import com.enonic.cloud.common.logwrappers.LoggedRunnable;

import static com.enonic.cloud.apis.ApiUtils.formatWebApplicationException;


@Value.Immutable
public abstract class CloudflareCommand
    extends LoggedRunnable
{
    protected abstract String action();

    protected abstract DnsRecord dnsRecord();

    @Override
    protected String beforeMessage()
    {
        return String.format( "CF: %s %s (type: %s, ttl: %d, cdn: %s) %s", action(), dnsRecord().name(), dnsRecord().type(),
                              dnsRecord().ttl(), dnsRecord().proxied(), dnsRecord().content() );
    }

    @Override
    public void run()
    {
        try
        {
            super.run();
        }
        catch ( WebApplicationException e )
        {
            throw formatWebApplicationException( e, String.format( "CF: %s failed", action() ) );
        }
    }
}
