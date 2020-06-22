package com.enonic.cloud.apis.cloudflare;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.cloudflare.service.DnsRecordService;
import com.enonic.cloud.apis.cloudflare.service.model.DnsRecord;

import static com.enonic.cloud.apis.ApiUtils.formatWebApplicationException;

@SuppressWarnings("CdiInjectionPointsInspection")
public class CloudflareFunctionProducer
{
    private static final Logger log = LoggerFactory.getLogger( CloudflareFunctionProducer.class );

    @Produces
    @Singleton
    public Function<CloudflareListParams, List<DnsRecord>> producerListFunction( @RestClient DnsRecordService service )
    {
        return ( params ) -> {
            try
            {
                return service.list( params.zoneId(), params.name(), params.type() ).result();
            }
            catch ( WebApplicationException e )
            {
                throw formatWebApplicationException( e, "CF: LIST failed" );
            }
        };
    }

    @Produces
    @Singleton
    @Named("create")
    public Function<DnsRecord, Runnable> producerCreateFunction( @RestClient DnsRecordService service )
    {
        return ( dnsRecord ) -> ImmutableCloudflareCommand.builder().
            action( "CREATE" ).
            dnsRecord( dnsRecord ).
            wrappedRunnable( () -> service.create( dnsRecord.zone_id(), dnsRecord ) ).
            build();
    }

    @Produces
    @Singleton
    @Named("update")
    public Function<DnsRecord, Runnable> producerUpdateFunction( @RestClient DnsRecordService service )
    {
        return ( dnsRecord ) -> ImmutableCloudflareCommand.builder().
            action( "UPDATE" ).
            dnsRecord( dnsRecord ).
            wrappedRunnable( () -> service.update( dnsRecord.zone_id(), dnsRecord.id(), dnsRecord ) ).
            build();
    }

    @Produces
    @Singleton
    @Named("delete")
    public Function<DnsRecord, Runnable> producerDeleteFunction( @RestClient DnsRecordService service )
    {
        return ( dnsRecord ) -> ImmutableCloudflareCommand.builder().
            action( "DELETE" ).
            dnsRecord( dnsRecord ).
            wrappedRunnable( () -> service.delete( dnsRecord.zone_id(), dnsRecord.id() ) ).
            build();
    }
}
