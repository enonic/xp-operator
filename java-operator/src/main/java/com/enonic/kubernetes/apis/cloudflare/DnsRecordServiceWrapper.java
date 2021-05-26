package com.enonic.kubernetes.apis.cloudflare;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.kubernetes.apis.cloudflare.service.DnsRecordService;
import com.enonic.kubernetes.apis.cloudflare.service.model.DnsRecord;

import static com.enonic.kubernetes.apis.ApiUtils.formatWebApplicationException;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Singleton
public class DnsRecordServiceWrapper
{
    private static final Logger log = LoggerFactory.getLogger( DnsRecordServiceWrapper.class );

    private DnsRecordService service;

    @Inject
    public DnsRecordServiceWrapper( @RestClient final DnsRecordService service )
    {
        singletonAssert(this, "constructor");
        this.service = service;
    }

    public List<DnsRecord> list( final String zone_identifier, final String name, final String type )
    {
        try
        {
            log.info( String.format( "CF: LIST (%s) %s", type == null ? "ALL" : type, name ) );
            return service.list( zone_identifier, name, type ).result();
        }
        catch ( WebApplicationException e )
        {
            throw formatWebApplicationException( e, "CF: LIST failed" );
        }
    }

    public Runnable create( final DnsRecord record )
    {
        return ImmutableCloudflareCommand.builder().
            action( "CREATE" ).
            dnsRecord( record ).
            wrappedRunnable( () -> service.create( record.zone_id(), record ) ).
            build();
    }

    public Runnable update( final DnsRecord record )
    {
        return ImmutableCloudflareCommand.builder().
            action( "UPDATE" ).
            dnsRecord( record ).
            wrappedRunnable( () -> service.update( record.zone_id(), record.id(), record ) ).
            build();
    }

    public Runnable delete( final DnsRecord record )
    {
        return ImmutableCloudflareCommand.builder().
            action( "DELETE" ).
            dnsRecord( record ).
            wrappedRunnable( () -> service.delete( record.zone_id(), record.id() ) ).
            build();
    }
}
